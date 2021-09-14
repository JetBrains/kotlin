/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

fun FirFunction.computeJvmSignature(typeConversion: (FirTypeRef) -> ConeKotlinType? = FirTypeRef::coneTypeSafe): String? {
    val containingClass = containingClass() ?: return null

    return SignatureBuildingComponents.signature(containingClass.classId, computeJvmDescriptor(typeConversion = typeConversion))
}

// TODO: `typeConversion` is only used for converting Java types into cone types, but shouldn't it be trivial
//   to construct a JVM descriptor from a Java type directly? The question is how to make the two paths consistent...
fun FirFunction.computeJvmDescriptor(
    customName: String? = null,
    includeReturnType: Boolean = true,
    typeConversion: (FirTypeRef) -> ConeKotlinType? = FirTypeRef::coneTypeSafe
): String = buildString {
    if (customName != null) {
        append(customName)
    } else {
        if (this@computeJvmDescriptor is FirSimpleFunction) {
            append(name.asString())
        } else {
            append("<init>")
        }
    }

    append("(")
    for (parameter in valueParameters) {
        typeConversion(parameter.returnTypeRef)?.let { appendConeType(it, typeConversion) }
    }
    append(")")

    if (includeReturnType) {
        if (this@computeJvmDescriptor !is FirSimpleFunction || returnTypeRef.isVoid()) {
            append("V")
        } else {
            typeConversion(returnTypeRef)?.let { appendConeType(it, typeConversion) }
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

private fun StringBuilder.appendConeType(coneType: ConeKotlinType, typeConversion: (FirTypeRef) -> ConeKotlinType?) {
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
        val baseClassId = type.lookupTag.classId
        // TODO: what about primitive arrays?
        val classId = JavaToKotlinClassMap.mapKotlinToJava(baseClassId.asSingleFqName().toUnsafe()) ?: baseClassId
        if (classId == StandardClassIds.Array) {
            append("[")
            type.typeArguments.forEach { typeArg ->
                when (typeArg) {
                    ConeStarProjection -> append("*")
                    is ConeKotlinTypeProjection -> appendConeType(typeArg.type, typeConversion)
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

    when (coneType) {
        is ConeClassErrorType -> Unit // TODO: just skipping it seems wrong
        is ConeClassLikeType -> {
            appendClassLikeType(coneType)
        }
        is ConeTypeParameterType -> {
            // TODO: 1. unannotated bounds are probably flexible, so this isn't right;
            //       2. shouldn't this always take the first bound and recurse if it's also a type parameter?
            coneType.lookupTag.typeParameterSymbol.fir.bounds.firstNotNullOfOrNull {
                val converted = typeConversion(it)
                if (converted is ConeClassLikeType) it to converted else null
            }?.let { (firBound, coneBound) ->
                // TODO: pretty sure Java type conversion does not produce either of these
                if (firBound !is FirImplicitNullableAnyTypeRef && firBound !is FirImplicitAnyTypeRef) {
                    appendClassLikeType(coneBound)
                    return
                }
            }
            append("Ljava/lang/Object;")
        }
        is ConeDefinitelyNotNullType -> appendConeType(coneType.original, typeConversion)
        is ConeFlexibleType -> appendConeType(coneType.lowerBound, typeConversion)
        else -> Unit // TODO: throw an error? should check that Java type conversion/enhancement can only produce these cone types
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
