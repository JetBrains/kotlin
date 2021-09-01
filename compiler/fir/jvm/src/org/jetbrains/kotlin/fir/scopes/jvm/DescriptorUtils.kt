/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

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
        appendConeType(typeConversion(parameter.returnTypeRef), typeConversion)
    }
    append(")")

    if (includeReturnType) {
        if (this@computeJvmDescriptor !is FirSimpleFunction || returnTypeRef.isVoid()) {
            append("V")
        } else {
            appendConeType(typeConversion(returnTypeRef), typeConversion)
        }
    }
}

private fun StringBuilder.appendClassId(classId: ClassId) {
    append("L")
        .append(classId.packageFqName.asString().replace(".", "/"))
        .append("/")
        .append(classId.relativeClassName)
        .append(";")
}

private fun StringBuilder.appendConeType(coneType: ConeKotlinType?, typeConversion: (FirTypeRef) -> ConeKotlinType?) {
    // `kotlin.Int` <-> `int`
    // `kotlin.Int..kotlin.Int?` <-> `Integer`
    // `@EnhancedNullability kotlin.Int` <-> `@NotNull Integer`
    if (coneType is ConeClassLikeType && !coneType.isNullable && !coneType.hasEnhancedNullability) {
        val classId = coneType.lookupTag.classId
        if (classId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
            PrimitiveType.getByShortName(classId.shortClassName.identifier)?.let {
                append(JvmPrimitiveType.get(it).desc)
                return
            }
        }
    }
    appendConeReferenceType(coneType, typeConversion, mutableSetOf())
}

private fun StringBuilder.appendConeReferenceType(
    coneType: ConeKotlinType?,
    typeConversion: (FirTypeRef) -> ConeKotlinType?,
    visited: MutableSet<FirTypeParameterSymbol>
) {
    when (coneType) {
        null -> append("Ljava/lang/Object;")
        is ConeClassErrorType -> append('*')
        is ConeClassLikeType -> {
            val baseClassId = coneType.lookupTag.classId
            if (baseClassId == StandardClassIds.Array) {
                // `Array<T>` -> `T[]`, where `T` is non-primitive, so e.g. `Array<Int>` is `Integer[]`.
                append('[').appendConeReferenceType(coneType.typeArguments.singleOrNull()?.type, typeConversion, visited)
                return
            } else if (baseClassId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
                // For primitive `T`, it should be `IntArray`, etc.
                PrimitiveType.getByShortArrayName(baseClassId.shortClassName.identifier)?.let {
                    append('[').append(JvmPrimitiveType.get(it).desc)
                    return
                }
            }
            appendClassId(JavaToKotlinClassMap.mapKotlinToJava(baseClassId.asSingleFqName().toUnsafe()) ?: baseClassId)
        }
        // `T extends A & B & ...` -> `A`; type parameters cannot have primitive upper bounds.
        is ConeTypeParameterType -> {
            val firstBound = coneType.lookupTag.typeParameterSymbol.takeIf { visited.add(it) }
                ?.fir?.bounds?.firstOrNull()?.let { typeConversion(it) }
            appendConeReferenceType(firstBound, typeConversion, visited)
        }
        // `T & Any` should only appear when `T` is a nullable-bounded type parameter.
        is ConeDefinitelyNotNullType -> appendConeReferenceType(coneType.original, typeConversion, visited)
        // `T..T?` is always a reference type; primitives are non-nullable.
        is ConeFlexibleType -> appendConeReferenceType(coneType.lowerBound, typeConversion, visited)
        else -> append('*') // TODO: throw an error? should check that Java type conversion/enhancement can only produce these cone types
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
