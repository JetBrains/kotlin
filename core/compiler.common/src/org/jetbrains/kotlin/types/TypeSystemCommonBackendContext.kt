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

    fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker?
    fun TypeConstructorMarker.isInlineClass(): Boolean
    fun TypeConstructorMarker.isInnerClass(): Boolean
    fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker
    fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker?

    fun KotlinTypeMarker.makeNullable(): KotlinTypeMarker =
        asSimpleType()?.withNullability(true) ?: this

    fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType?
    fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType?

    fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean
    fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe?

    fun TypeParameterMarker.getName(): Name
    fun TypeParameterMarker.isReified(): Boolean

    fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean
}
