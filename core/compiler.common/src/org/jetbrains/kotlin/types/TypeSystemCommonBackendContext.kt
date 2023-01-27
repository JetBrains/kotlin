/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.*

interface TypeSystemCommonBackendContext : TypeSystemContext {
    fun nullableAnyType(): SimpleTypeMarker
    fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker
    fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean

    fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean

    fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean

    /**
     * @return value of the first argument of the annotation with the given [fqName], if the annotation is present and
     * the argument is of a primitive type or a String, or null otherwise.
     *
     * Note that this method returns null if no arguments are provided, even if the corresponding annotation parameter has a default value.
     *
     * TODO: provide a more granular & elaborate API here to reduce confusion
     */
    fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any?

    fun TypeConstructorMarker.isInlineClass(): Boolean
    fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean
    fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>?
    fun TypeConstructorMarker.isInnerClass(): Boolean
    fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker
    fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker?
    fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker?

    fun KotlinTypeMarker.makeNullable(): KotlinTypeMarker =
        asSimpleType()?.withNullability(true) ?: this

    fun KotlinTypeMarker.makeNonNullable(): KotlinTypeMarker =
        asSimpleType()?.withNullability(false) ?: this

    fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType?
    fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType?

    fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean
    fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe?

    fun TypeParameterMarker.getName(): Name
    fun TypeParameterMarker.isReified(): Boolean

    fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean
}

interface TypeSystemCommonBackendContextForTypeMapping : TypeSystemCommonBackendContext {
    fun TypeConstructorMarker.isTypeParameter(): Boolean
    fun TypeConstructorMarker.asTypeParameter(): TypeParameterMarker
    fun TypeConstructorMarker.defaultType(): KotlinTypeMarker
    fun TypeConstructorMarker.isScript(): Boolean

    fun SimpleTypeMarker.isSuspendFunction(): Boolean
    fun SimpleTypeMarker.isKClass(): Boolean

    fun KotlinTypeMarker.isRawType(): Boolean

    fun TypeConstructorMarker.typeWithArguments(arguments: List<KotlinTypeMarker>): SimpleTypeMarker
    fun TypeConstructorMarker.typeWithArguments(vararg arguments: KotlinTypeMarker): SimpleTypeMarker {
        return typeWithArguments(arguments.toList())
    }

    fun TypeArgumentMarker.adjustedType(): KotlinTypeMarker {
        if (this.isStarProjection()) return nullableAnyType()
        return getType()
    }

    fun TypeParameterMarker.representativeUpperBound(): KotlinTypeMarker

    fun continuationTypeConstructor(): TypeConstructorMarker
    fun functionNTypeConstructor(n: Int): TypeConstructorMarker

    fun KotlinTypeMarker.getNameForErrorType(): String?
}
