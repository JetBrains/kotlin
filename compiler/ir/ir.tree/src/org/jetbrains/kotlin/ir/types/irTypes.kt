/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.*

private fun IrType.withNullability(newNullability: Boolean): IrType =
    when (this) {
        is IrSimpleType -> withNullability(newNullability)
        else -> this
    }

private fun IrSimpleType.withNullability(newNullability: Boolean): IrSimpleType {
    val requiredNullability = if (newNullability) SimpleTypeNullability.MARKED_NULLABLE else SimpleTypeNullability.DEFINITELY_NOT_NULL
    return if (nullability == requiredNullability)
        this
    else
        buildSimpleType {
            nullability = requiredNullability
            kotlinType = originalKotlinType?.run {
                if (newNullability) {
                    TypeUtils.makeNullable(this)
                } else {
                    DefinitelyNotNullType.makeDefinitelyNotNull(this.unwrap()) ?: TypeUtils.makeNotNullable(this)
                }
            }
        }
}

fun IrType.addAnnotations(newAnnotations: List<IrConstructorCall>): IrType =
    if (newAnnotations.isEmpty())
        this
    else when (this) {
        is IrSimpleType ->
            toBuilder().apply {
                annotations = annotations + newAnnotations
            }.buildSimpleType()
        is IrDynamicType ->
            IrDynamicTypeImpl(null, annotations + newAnnotations, Variance.INVARIANT)
        else ->
            this
    }

fun IrType.removeAnnotations(predicate: (IrConstructorCall) -> Boolean): IrType =
    when (this) {
        is IrSimpleType ->
            toBuilder().apply {
                annotations = annotations.filterNot(predicate)
            }.buildSimpleType()
        is IrDynamicType ->
            IrDynamicTypeImpl(null, annotations.filterNot(predicate), Variance.INVARIANT)
        else ->
            this
    }

fun IrType.removeAnnotations(): IrType =
    when (this) {
        is IrSimpleType ->
            toBuilder().apply {
                annotations = emptyList()
            }.buildSimpleType()
        is IrDynamicType ->
            IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
        else ->
            this
    }

val IrType.classifierOrFail: IrClassifierSymbol
    get() = classifierOrNull ?: error("Can't get classifier of ${render()}")

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = when (this) {
        is IrSimpleType -> classifier
        else -> null
    }

val IrType.classOrNull: IrClassSymbol?
    get() =
        when (val classifier = classifierOrNull) {
            is IrClassSymbol -> classifier
            is IrScriptSymbol -> classifier.owner.targetClass
            else -> null
        }

val IrType.classFqName: FqName?
    get() = classOrNull?.owner?.fqNameWhenAvailable

val IrTypeArgument.typeOrNull: IrType? get() = (this as? IrTypeProjection)?.type

fun IrType.makeNotNull() = withNullability(false)

fun IrType.makeNullable() = withNullability(true)

fun IrType.mergeNullability(other: IrType) = when (other) {
    is IrSimpleType -> when (other.nullability) {
        SimpleTypeNullability.MARKED_NULLABLE -> makeNullable()
        SimpleTypeNullability.NOT_SPECIFIED -> this
        SimpleTypeNullability.DEFINITELY_NOT_NULL -> makeNotNull()
    }
    else -> this
}

@ObsoleteDescriptorBasedAPI
fun IrType.toKotlinType(): KotlinType {
    originalKotlinType?.let {
        return it
    }

    return when (this) {
        is IrSimpleType -> makeKotlinType(classifier, arguments, nullability == SimpleTypeNullability.MARKED_NULLABLE)
        else -> TODO(toString())
    }
}

fun IrType.getClass(): IrClass? =
    classOrNull?.owner

fun IrClassSymbol.createType(hasQuestionMark: Boolean, arguments: List<IrTypeArgument>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        hasQuestionMark,
        arguments,
        emptyList()
    )

@ObsoleteDescriptorBasedAPI
private fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean
): SimpleType {
    val kotlinTypeArguments = arguments.mapIndexed { index, it ->
        when (it) {
            is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
            is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
            else -> error(it)
        }
    }
    return classifier.descriptor.defaultType.replace(newArguments = kotlinTypeArguments).makeNullableAsSpecified(hasQuestionMark)
}

val IrClassifierSymbol.defaultType: IrType
    get() = when (this) {
        is IrClassSymbol -> owner.defaultType
        is IrTypeParameterSymbol -> owner.defaultType
        else -> error("Unexpected classifier symbol type $this")
    }

val IrTypeParameter.defaultType: IrType
    get() = IrSimpleTypeImpl(
        symbol,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments = emptyList(),
        annotations = emptyList()
    )

val IrClassSymbol.starProjectedType: IrSimpleType
    get() = IrSimpleTypeImpl(
        this,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments = owner.typeConstructorParameters.map { IrStarProjectionImpl }.toList(),
        annotations = emptyList()
    )

val IrClass.typeConstructorParameters: Sequence<IrTypeParameter>
    get() =
        generateSequence(this as IrTypeParametersContainer) { current ->
            val parent = current.parent as? IrTypeParametersContainer
            when {
                parent is IrSimpleFunction && parent.isPropertyAccessor -> {
                    // KT-42151
                    // Property type parameters for local classes declared inside property accessors are not captured in FE descriptors.
                    // In order to match type parameters against type arguments in IR types translated from KotlinTypes,
                    // we should stop on property accessor here.
                    // NB this can potentially cause problems with inline properties with reified type parameters.
                    // Ideally this should be fixed in FE.
                    null
                }
                current is IrSimpleFunction && current.isStatic -> {
                    // Static functions don't capture type parameters.
                    null
                }
                current.isAnonymousObject -> {
                    // Anonymous classes don't capture type parameters.
                    null
                }
                parent is IrClass && current is IrClass && !current.isInner ->
                    null
                // Inline class constructor inherits the same type parameters as the inline class itself
                current.isJvmInlineClassConstructor ->
                    null
                else ->
                    parent
            }
        }.flatMap { it.typeParameters }

fun IrClassifierSymbol.typeWithParameters(parameters: List<IrTypeParameter>): IrSimpleType =
    typeWith(parameters.map { it.defaultType })

fun IrClassifierSymbol.typeWith(vararg arguments: IrType): IrSimpleType = typeWith(arguments.toList())

fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        SimpleTypeNullability.NOT_SPECIFIED,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun IrClassifierSymbol.typeWithArguments(arguments: List<IrTypeArgument>): IrSimpleType =
    IrSimpleTypeImpl(this, SimpleTypeNullability.NOT_SPECIFIED, arguments, emptyList())

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun IrClass.typeWith(vararg arguments: IrType) = this.symbol.typeWith(arguments.toList())
