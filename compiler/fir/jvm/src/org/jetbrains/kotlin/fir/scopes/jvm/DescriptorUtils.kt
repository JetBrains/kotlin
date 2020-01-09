/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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
                                append("Ljava/lang/Object")
                            } else {
                                appendClass(representative.classifier as JavaClass)
                            }
                        }
                        else -> return
                    }
                    append(";")
                }
            }
        }
    }
}

private fun StringBuilder.appendConeType(coneType: ConeKotlinType) {
    fun appendClassLikeType(type: ConeClassLikeType) {
        append("L")
        val classId = type.lookupTag.classId
        append(classId.packageFqName.asString().replace(".", "/"))
        append("/")
        append(classId.relativeClassName)
    }

    if (coneType is ConeClassErrorType) return
    when (coneType) {
        is ConeClassLikeType -> {
            appendClassLikeType(coneType)
        }
        is ConeTypeParameterType -> {
            val representative = coneType.lookupTag.typeParameterSymbol.fir.bounds.firstOrNull {
                (it as? FirResolvedTypeRef)?.type is ConeClassLikeType
            }
            if (representative == null) {
                append("Ljava/lang/Object")
            } else {
                appendClassLikeType(representative.coneTypeUnsafe())
            }
            append(coneType.lookupTag.name)
        }
    }
    append(";")
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
