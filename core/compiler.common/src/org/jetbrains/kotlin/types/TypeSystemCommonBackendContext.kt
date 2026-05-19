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
import org.jetbrains.kotlin.types.typeUtil.getEffectiveVariance

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
    fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, RigidTypeMarker>>?
    fun TypeConstructorMarker.isInnerClass(): Boolean
    fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker

    fun KotlinTypeMarker.getUnsubstitutedUnderlyingType(): KotlinTypeMarker?
    fun typeSubstitutorForUnderlyingType(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker =
        typeSubstitutorByTypeConstructor(map)

    fun KotlinTypeMarker.makeNullable(): KotlinTypeMarker =
        asRigidType()?.withNullability(true) ?: this
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

    fun RigidTypeMarker.isSuspendFunction(): Boolean
    fun RigidTypeMarker.isKClass(): Boolean

    fun TypeConstructorMarker.typeWithArguments(arguments: List<KotlinTypeMarker>): SimpleTypeMarker
    fun TypeConstructorMarker.typeWithArguments(vararg arguments: KotlinTypeMarker): SimpleTypeMarker {
        return typeWithArguments(arguments.toList())
    }

    fun TypeArgumentMarker.adjustedType(): KotlinTypeMarker {
        return getType() ?: nullableAnyType()
    }

    fun TypeParameterMarker.representativeUpperBound(): KotlinTypeMarker

    fun continuationTypeConstructor(): TypeConstructorMarker
    fun functionNTypeConstructor(n: Int): TypeConstructorMarker

    fun KotlinTypeMarker.getNameForErrorType(): String?
}

fun TypeSystemCommonBackendContext.isMostPreciseContravariantArgument(type: KotlinTypeMarker): Boolean =
    type.typeConstructor().isAnyConstructor()

fun TypeSystemCommonBackendContext.isMostPreciseCovariantArgument(type: KotlinTypeMarker): Boolean =
    !canHaveSubtypesIgnoringNullability(type)

private fun TypeSystemCommonBackendContext.canHaveSubtypesIgnoringNullability(kotlinType: KotlinTypeMarker): Boolean {
    val constructor = kotlinType.typeConstructor()

    if (!constructor.isClassTypeConstructor() || !constructor.isFinalClassOrEnumEntryOrAnnotationClassConstructor()) return true

    for (i in 0 until constructor.parametersCount()) {
        val parameter = constructor.getParameter(i)
        val argument = kotlinType.getArgument(i)

        val type = argument.getType() ?: return true
        val projectionKind = argument.getVariance().convertVariance()

        val effectiveVariance = getEffectiveVariance(parameter.getVariance().convertVariance(), projectionKind)
        if (effectiveVariance == Variance.OUT_VARIANCE && !isMostPreciseCovariantArgument(type)) return true
        if (effectiveVariance == Variance.IN_VARIANCE && !isMostPreciseContravariantArgument(type)) return true
    }

    return false
}
