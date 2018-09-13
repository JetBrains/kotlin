/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun IrType.withHasQuestionMark(hasQuestionMark: Boolean): IrType =
    when (this) {
        is IrSimpleType ->
            if (this.hasQuestionMark == hasQuestionMark)
                this
            else
                IrSimpleTypeImpl(
                    makeKotlinType(classifier, arguments, hasQuestionMark),
                    classifier,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
        else -> this
    }

val IrType.classifierOrFail: IrClassifierSymbol
    get() = cast<IrSimpleType>().classifier

val IrType.classifierOrNull: IrClassifierSymbol?
    get() = safeAs<IrSimpleType>()?.classifier

fun IrType.makeNotNull() =
    if (this is IrSimpleType && this.hasQuestionMark)
        IrSimpleTypeImpl(
            makeKotlinType(classifier, arguments, false),
            classifier,
            false,
            arguments,
            annotations,
            Variance.INVARIANT
        )
    else
        this

fun IrType.makeNullable() =
    if (this is IrSimpleType && !this.hasQuestionMark)
        IrSimpleTypeImpl(
            makeKotlinType(classifier, arguments, true),
            classifier,
            true,
            arguments,
            annotations,
            Variance.INVARIANT
        )
    else
        this

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
    (this.classifierOrNull as? IrClassSymbol)?.owner

fun IrClassSymbol.createType(hasQuestionMark: Boolean, arguments: List<IrTypeArgument>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        hasQuestionMark,
        arguments,
        emptyList()
    )

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

fun ClassifierDescriptor.toIrType(hasQuestionMark: Boolean = false, symbolTable: SymbolTable? = null): IrType {
    val symbol = getSymbol(symbolTable)
    return IrSimpleTypeImpl(defaultType, symbol, hasQuestionMark, listOf(), listOf())
}

val IrTypeParameter.defaultType: IrType get() = symbol.owner.defaultType

fun IrClassifierSymbol.typeWith(vararg arguments: IrType): IrSimpleType = typeWith(arguments.toList())

fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        false,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun KotlinType.toIrType(symbolTable: SymbolTable? = null): IrType? {
    if (isDynamic()) return IrDynamicTypeImpl(this, listOf(), Variance.INVARIANT)

    val symbol = constructor.declarationDescriptor?.getSymbol(symbolTable) ?: return null

    val arguments = this.arguments.map { projection ->
        when (projection) {
            is TypeProjectionImpl -> IrTypeProjectionImpl(projection.type.toIrType(symbolTable)!!, projection.projectionKind)
            is StarProjectionImpl -> IrStarProjectionImpl
            else -> error(projection)
        }
    }

    // TODO
    val annotations = listOf()
    return IrSimpleTypeImpl(this, symbol, isMarkedNullable, arguments, annotations)
}

// TODO: this function creates unbound symbol which is the great source of problems
private fun ClassifierDescriptor.getSymbol(symbolTable: SymbolTable?): IrClassifierSymbol = when (this) {
    is ClassDescriptor -> symbolTable?.referenceClass(this) ?: IrClassSymbolImpl(this)
    is TypeParameterDescriptor -> symbolTable?.referenceTypeParameter(this) ?: IrTypeParameterSymbolImpl(this)
    else -> TODO()
}