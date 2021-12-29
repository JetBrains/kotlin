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
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun IrType.withHasQuestionMark(newHasQuestionMark: Boolean): IrType =
    when (this) {
        is IrSimpleType -> withHasQuestionMark(newHasQuestionMark)
        else -> this
    }

fun IrSimpleType.withHasQuestionMark(newHasQuestionMark: Boolean): IrSimpleType =
    if (this.hasQuestionMark == newHasQuestionMark)
        this
    else
        buildSimpleType {
            hasQuestionMark = newHasQuestionMark
            kotlinType = originalKotlinType?.run {
                if (newHasQuestionMark) makeNullable() else makeNotNullable()
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
    get() = cast<IrSimpleType>().classifier

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = safeAs<IrSimpleType>()?.classifier

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

fun IrType.makeNotNull() =
    if (this is IrSimpleType && this.hasQuestionMark) {
        buildSimpleType {
            kotlinType = originalKotlinType?.makeNotNullable()
            hasQuestionMark = false
        }
    } else {
        this
    }

fun IrType.makeNullable(): IrType =
    when (this) {
        is IrSimpleType -> {
            if (this.hasQuestionMark)
                this
            else
                buildSimpleType {
                    kotlinType = originalKotlinType?.makeNullable()
                    hasQuestionMark = true
                }
        }
        is IrDefinitelyNotNullType -> {
            // '{ T & Any }?' => 'T?'
            this.original.makeNullable()
        }
        else ->
            this
    }

@ObsoleteDescriptorBasedAPI
fun IrType.toKotlinType(): KotlinType {
    originalKotlinType?.let {
        return it
    }

    return when (this) {
        is IrSimpleType -> makeKotlinType(classifier, arguments, hasQuestionMark)
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
        hasQuestionMark = false,
        arguments = emptyList(),
        annotations = emptyList()
    )

val IrClassSymbol.starProjectedType: IrSimpleType
    get() = IrSimpleTypeImpl(
        this,
        hasQuestionMark = false,
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
        false,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun IrClassifierSymbol.typeWithArguments(arguments: List<IrTypeArgument>): IrSimpleType =
    IrSimpleTypeImpl(this, false, arguments, emptyList())

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun IrClass.typeWith(vararg arguments: IrType) = this.symbol.typeWith(arguments.toList())
