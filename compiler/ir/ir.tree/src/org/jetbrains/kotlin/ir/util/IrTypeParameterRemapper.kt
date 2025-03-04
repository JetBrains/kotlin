/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * After moving an [org.jetbrains.kotlin.ir.IrElement], some type parameter references within it may become out of scope.
 * This remapper restores validity by redirecting those references to new type parameters.
 */
abstract class AbstractIrTypeParameterRemapper<in D> : TypeRemapperWithData<D> {
    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer, data: D) {}
    override fun leaveScope(data: D) {}

    abstract fun remapTypeParameter(typeParameter: IrTypeParameter, data: D): IrTypeParameterSymbol?

    override fun remapType(type: IrType, data: D): IrType =
        if (type !is IrSimpleType)
            type
        else
            IrSimpleTypeImpl(
                type.classifier.remap(data),
                type.nullability,
                type.arguments.memoryOptimizedMap { it.remap(data) },
                type.annotations,
                type.abbreviation?.remap(data)
            ).apply {
                annotations.forEach { it.remapTypes(this@AbstractIrTypeParameterRemapper, data) }
            }

    private fun IrClassifierSymbol.remap(data: D) =
        (owner as? IrTypeParameter)?.let {
            remapTypeParameter(
                it,
                data
            )
        }
            ?: this

    private fun IrTypeArgument.remap(data: D) =
        when (this) {
            is IrTypeProjection -> makeTypeProjection(remapType(type, data), variance)
            is IrStarProjection -> this
        }

    private fun IrTypeAbbreviation.remap(data: D)=
        IrTypeAbbreviationImpl(
            typeAlias,
            hasQuestionMark,
            arguments.memoryOptimizedMap { it.remap(data) },
            annotations
        ).apply {
            annotations.forEach { it.remapTypes(this@AbstractIrTypeParameterRemapper, data) }
        }
}

class MapBasedIrTypeParameterRemapper(
    private val typeParameterMap: Map<IrTypeParameter, IrTypeParameter>
) : AbstractIrTypeParameterRemapper<Nothing?>(), TypeRemapper {
    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer, data: Nothing?) {
        enterScope(irTypeParametersContainer)
    }

    override fun leaveScope() {}

    override fun leaveScope(data: Nothing?) {
        leaveScope()
    }

    override fun remapType(type: IrType): IrType = super<AbstractIrTypeParameterRemapper>.remapType(type, null)

    override fun remapType(type: IrType, data: Nothing?): IrType = remapType(type)

    override fun remapTypeParameter(typeParameter: IrTypeParameter, data: Nothing?): IrTypeParameterSymbol? =
        typeParameterMap[typeParameter]?.symbol
}
