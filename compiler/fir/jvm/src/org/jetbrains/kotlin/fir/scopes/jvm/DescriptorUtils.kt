/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun FirFunction<*>.computeJvmSignature(typeConversion: (FirTypeRef) -> ConeKotlinType? = FirTypeRef::coneTypeSafe): String? {
    val containingClass = containingClass() ?: return null

    return SignatureBuildingComponents.signature(containingClass.classId, computeJvmDescriptor(typeConversion = typeConversion))
}

fun FirFunction<*>.computeJvmDescriptor(
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
        typeConversion(parameter.returnTypeRef)?.let(this::appendConeType)
    }
    append(")")

    if (includeReturnType) {
        if (this@computeJvmDescriptor !is FirSimpleFunction || returnTypeRef.isVoid()) {
            append("V")
        } else {
            typeConversion(returnTypeRef)?.let(this::appendConeType)
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
        val baseClassId = type.lookupTag.classId
        val classId = JavaToKotlinClassMap.mapKotlinToJava(baseClassId.asSingleFqName().toUnsafe()) ?: baseClassId
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
