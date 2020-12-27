/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun FirFunction<*>.computeJvmDescriptorReplacingKotlinToJava(): String =
    computeJvmDescriptor()
        .replace("kotlin/Any", "java/lang/Object")
        .replace("kotlin/String", "java/lang/String")
        .replace("kotlin/Throwable", "java/lang/Throwable")

fun FirFunction<*>.computeJvmDescriptor(): String = buildString {
    if (this@computeJvmDescriptor is FirSimpleFunction) {
        append(name.asString())
    } else {
        append("<init>")
    }

    append("(")
    for (parameter in valueParameters) {
        appendErasedType(parameter.returnTypeRef)
    }
    append(")")

    if (this@computeJvmDescriptor !is FirSimpleFunction || returnTypeRef.isVoid()) {
        append("V")
    } else {
        appendErasedType(returnTypeRef)
    }
}

// TODO: primitive types, arrays, etc.
private fun StringBuilder.appendErasedType(typeRef: FirTypeRef) {
    fun appendClass(klass: JavaClass) {
        klass.fqName?.let {
            append("L")
            append(it.asString().replace(".", "/"))
            append(";")
        }
    }

    when (typeRef) {
        is FirResolvedTypeRef -> appendConeType(typeRef.type)
        is FirJavaTypeRef -> {
            when (val javaType = typeRef.type) {
                is JavaClassifierType -> {
                    when (val classifier = javaType.classifier) {
                        is JavaClass -> appendClass(classifier)
                        is JavaTypeParameter -> {
                            val representative = classifier.upperBounds.firstOrNull { it.classifier is JavaClass }
                            if (representative == null) {
                                append("Ljava/lang/Object;")
                            } else {
                                appendClass(representative.classifier as JavaClass)
                            }
                        }
                        else -> return
                    }
                }
            }
        }
    }
}

private val PRIMITIVE_TYPE_SIGNATURE: Map<String, String> = mapOf(
    "Boolean" to "Z",
    "Byte" to "B",
    "Char" to "C",
    "Short" to "S",
    "Int" to "I",
    "Long" to "J",
    "Float" to "F",
    "Double" to "D",
)

private fun StringBuilder.appendConeType(coneType: ConeKotlinType) {
    (coneType as? ConeClassLikeType)?.let {
        val classId = it.lookupTag.classId
        if (classId.packageFqName.toString() == "kotlin") {
            PRIMITIVE_TYPE_SIGNATURE[classId.shortClassName.identifier]?.let { signature ->
                append(signature)
                return
            }
        }
    }

    fun appendClassLikeType(type: ConeClassLikeType) {
        val classId = type.lookupTag.classId
        if (classId == StandardClassIds.Array) {
            append("[")
            type.typeArguments.forEach { typeArg ->
                when (typeArg) {
                    ConeStarProjection -> append("*")
                    is ConeKotlinTypeProjection -> appendConeType(typeArg.type)
                }
            }
        } else {
            append("L")
            append(classId.packageFqName.asString().replace(".", "/"))
            append("/")
            append(classId.relativeClassName)
            append(";")
        }
    }

    if (coneType is ConeClassErrorType) return
    when (coneType) {
        is ConeClassLikeType -> {
            appendClassLikeType(coneType)
        }
        is ConeTypeParameterType -> {
            val representative = coneType.lookupTag.typeParameterSymbol.fir.bounds.firstOrNull {
                it.coneType is ConeClassLikeType
            }
            if (representative == null || representative is FirImplicitNullableAnyTypeRef || representative is FirImplicitAnyTypeRef) {
                append("Ljava/lang/Object;")
            } else {
                appendClassLikeType(representative.coneTypeUnsafe())
            }
        }
        is ConeDefinitelyNotNullType -> {
            appendConeType(coneType.original)
        }
        is ConeFlexibleType -> {
            appendConeType(coneType.lowerBound)
        }
    }
}

private val unitClassId = ClassId.topLevel(FqName("kotlin.Unit"))

private fun FirTypeRef.isVoid(): Boolean {
    return when (this) {
        is FirJavaTypeRef -> {
            val type = type
            type is JavaPrimitiveType && type.type == null
        }
        is FirResolvedTypeRef -> {
            val type = type
            type is ConeClassLikeType && type.lookupTag.classId == unitClassId
        }
        else -> false
    }
}
