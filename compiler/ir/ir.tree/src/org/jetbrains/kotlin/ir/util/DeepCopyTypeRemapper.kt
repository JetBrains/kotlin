/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class DeepCopyTypeRemapper(
    private val symbolRemapper: ReferencedSymbolRemapper,
    private val typeSubstitutor: IrTypeSubstitutor? = null,
) : TypeRemapper {

    lateinit var deepCopy: DeepCopyIrTreeWithSymbols

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        // TODO
    }

    override fun leaveScope() {
        // TODO
    }

    override fun remapType(type: IrType): IrType {
        val substitutedType = typeSubstitutor?.substitute(type) ?: type
        return when (substitutedType) {
            is IrSimpleType -> IrSimpleTypeImpl(
                symbolRemapper.getReferencedClassifier(substitutedType.classifier),
                substitutedType.nullability,
                substitutedType.arguments.memoryOptimizedMap { remapTypeArgument(it) },
                substitutedType.annotations.memoryOptimizedMap { it.transform(deepCopy, null) as IrConstructorCall },
                substitutedType.abbreviation?.remapTypeAbbreviation()
            )
            else -> substitutedType
        }
    }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        when (typeArgument) {
            is IrTypeProjection -> makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
            is IrStarProjection -> typeArgument
        }

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.memoryOptimizedMap { remapTypeArgument(it) },
            annotations
        )
}
