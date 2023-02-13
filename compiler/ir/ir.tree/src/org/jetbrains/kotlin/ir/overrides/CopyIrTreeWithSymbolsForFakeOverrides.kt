/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.linkage.partial.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// This is basically modelled after the inliner copier.
class CopyIrTreeWithSymbolsForFakeOverrides(
    private val overridableMember: IrOverridableMember,
    typeArguments: Map<IrTypeParameterSymbol, IrType>,
    private val parent: IrClass,
    unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) {
    fun copy(): IrOverridableMember {
        // Create new symbols.
        overridableMember.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        // Copy IR.
        val result = overridableMember.transform(
            if (parent.isEffectivelyExternal()) copierMakingExternal else copier,
            data = null
        ) as IrOverridableMember

        result.patchDeclarationParents(parent)

        return result
    }

    private inner class FakeOverrideTypeRemapper(
        val symbolRemapper: SymbolRemapper,
        val typeArguments: Map<IrTypeParameterSymbol, IrType>
    ) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

        override fun leaveScope() {}

        private fun remapTypeArguments(arguments: List<IrTypeArgument>) =
            arguments.map { argument ->
                (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                    ?: argument
            }

        override fun remapType(type: IrType): IrType {
            if (type !is IrSimpleType) return type

            return when (val substitutedType = typeArguments[type.classifier]) {
                is IrDynamicType -> substitutedType
                is IrSimpleType -> substitutedType.mergeNullability(type)
                else -> type.buildSimpleType {
                    kotlinType = null
                    classifier = symbolRemapper.getReferencedClassifier(type.classifier)
                    arguments = remapTypeArguments(type.arguments)
                    annotations = type.annotations.map { it.transform(copier, null) as IrConstructorCall }
                }
            }
        }
    }

    private class FakeOverrideSymbolRemapperImpl(
        private val typeArguments: Map<IrTypeParameterSymbol, IrType>,
        descriptorsRemapper: DescriptorsRemapper
    ) :
        DeepCopySymbolRemapper(descriptorsRemapper) {

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments[result]?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper =
        FakeOverrideSymbolRemapperImpl(
            typeArguments,
            NullDescriptorsRemapper
        )

    private val copier = FakeOverrideCopier(
        symbolRemapper,
        FakeOverrideTypeRemapper(symbolRemapper, typeArguments),
        SymbolRenamer.DEFAULT,
        makeExternal = false,
        parent,
        unimplementedOverridesStrategy
    )

    private val copierMakingExternal = FakeOverrideCopier(
        symbolRemapper,
        FakeOverrideTypeRemapper(symbolRemapper, typeArguments),
        SymbolRenamer.DEFAULT,
        makeExternal = true,
        parent,
        unimplementedOverridesStrategy
    )
}
