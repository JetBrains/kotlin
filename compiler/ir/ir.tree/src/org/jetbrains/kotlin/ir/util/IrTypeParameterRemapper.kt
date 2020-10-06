/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection

/* After moving an IrElement, some type parameter references within it may become out of scope.
   This remapper restores validity by redirecting those references to new type parameters.
 */
class IrTypeParameterRemapper(
    val typeParameterMap: Map<IrTypeParameter, IrTypeParameter>
) : TypeRemapper {
    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}
    override fun leaveScope() {}

    override fun remapType(type: IrType): IrType =
        if (type !is IrSimpleType)
            type
        else
            IrSimpleTypeImpl(
                null,
                type.classifier.remap(),
                type.hasQuestionMark,
                type.arguments.map { it.remap() },
                type.annotations,
                type.abbreviation?.remap()
            ).apply {
                annotations.forEach { it.remapTypes(this@IrTypeParameterRemapper) }
            }

    private fun IrClassifierSymbol.remap() =
        (owner as? IrTypeParameter)?.let { typeParameterMap[it]?.symbol }
            ?: this

    private fun IrTypeArgument.remap() =
        if (this is IrTypeProjection)
            makeTypeProjection(remapType(type), variance)
        else
            this

    private fun IrTypeAbbreviation.remap() =
        IrTypeAbbreviationImpl(
            typeAlias,
            hasQuestionMark,
            arguments.map { it.remap() },
            annotations
        ).apply {
            annotations.forEach { it.remapTypes(this@IrTypeParameterRemapper) }
        }
}