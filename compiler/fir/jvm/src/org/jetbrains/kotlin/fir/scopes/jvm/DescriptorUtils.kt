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

fun FirFunction.computeJvmSignature(): String? {
    val containingClass = containingClass() ?: return null
    return SignatureBuildingComponents.signature(containingClass.classId, computeJvmDescriptor())
}

fun FirFunction.computeJvmDescriptor(customName: String? = null, includeReturnType: Boolean = true): String = buildString {
    append(customName ?: if (this@computeJvmDescriptor is FirSimpleFunction) name.asString() else "<init>")
    append("(")
    for (parameter in valueParameters) {
        appendFirTypeRef(parameter.returnTypeRef)
    }
    append(")")

    if (includeReturnType) {
        if (this@computeJvmDescriptor !is FirSimpleFunction || returnTypeRef.isUnit) {
            append("V")
        } else {
            appendFirTypeRef(returnTypeRef)
        }
    }
}

private fun StringBuilder.appendFirTypeRef(firTypeRef: FirTypeRef) {
    if (firTypeRef is FirJavaTypeRef) {
        appendJavaType(firTypeRef.type, mutableSetOf())
    } else {
        appendConeType(firTypeRef.coneTypeSafe())
    }
}

private fun StringBuilder.appendClassId(classId: ClassId) {
    append("L")
        .append(classId.packageFqName.asString().replace(".", "/"))
        .append("/")
        .append(classId.relativeClassName)
        .append(";")
}

private fun StringBuilder.appendJavaType(javaType: JavaType?, visited: MutableSet<JavaTypeParameter>) {
    when (javaType) {
        is JavaPrimitiveType -> append(javaType.type?.let { JvmPrimitiveType.get(it).desc } ?: "V")
        is JavaWildcardType -> appendJavaType(javaType.bound.takeIf { javaType.isExtends }, visited)
        is JavaArrayType -> append('[').appendJavaType(javaType.componentType, visited)
        is JavaClassifierType -> {
            when (val classifier = javaType.classifier) {
                is JavaClass -> appendClassId(classifier.classId!!)
                is JavaTypeParameter -> appendJavaType(classifier.takeIf { visited.add(it) }?.upperBounds?.firstOrNull(), visited)
                else -> appendClassId(ClassId.topLevel(FqName(javaType.classifierQualifiedName)))
            }
        }
        else -> append("Ljava/lang/Object;")
    }
}

private fun StringBuilder.appendConeType(coneType: ConeKotlinType?) {
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
    appendConeReferenceType(coneType, mutableSetOf())
}

private fun StringBuilder.appendConeReferenceType(coneType: ConeKotlinType?, visited: MutableSet<FirTypeParameterSymbol>) {
    when (coneType) {
        null -> append("Ljava/lang/Object;")
        is ConeClassErrorType -> append('*')
        is ConeClassLikeType -> {
            val baseClassId = coneType.lookupTag.classId
            if (baseClassId == StandardClassIds.Array) {
                // `Array<T>` -> `T[]`, where `T` is non-primitive, so e.g. `Array<Int>` is `Integer[]`.
                append('[').appendConeReferenceType(coneType.typeArguments.singleOrNull()?.type, visited)
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
            val firstBound = coneType.lookupTag.typeParameterSymbol.takeIf { visited.add(it) }?.fir?.bounds?.firstOrNull()
            if (firstBound is FirJavaTypeRef) {
                appendJavaType(firstBound.type, mutableSetOf())
            } else {
                appendConeReferenceType(firstBound?.coneTypeSafe(), visited)
            }
        }
        // `T & Any` should only appear when `T` is a nullable-bounded type parameter.
        is ConeDefinitelyNotNullType -> appendConeReferenceType(coneType.original, visited)
        // `T..T?` is always a reference type; primitives are non-nullable.
        is ConeFlexibleType -> appendConeReferenceType(coneType.lowerBound, visited)
        else -> append('*') // TODO: throw an error? should check that Java type conversion/enhancement can only produce these cone types
    }
}
