/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.EnhancedNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleNullability
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedMap

// This is basically modelled after the inliner copier.
class CopyIrTreeWithSymbolsForFakeOverrides(
    private val overridableMember: IrOverridableMember,
    typeArguments: Map<IrTypeParameterSymbol, IrType>,
    private val parent: IrClass,
    unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) {
    private val symbolRemapper = FakeOverrideSymbolRemapperImpl(typeArguments, NullDescriptorsRemapper)

    private val copier = FakeOverrideCopier(
        symbolRemapper,
        FakeOverrideTypeRemapper(symbolRemapper, typeArguments),
        SymbolRenamer.DEFAULT,
        parent,
        unimplementedOverridesStrategy
    )

    fun copy(): IrOverridableMember {
        overridableMember.acceptVoid(symbolRemapper)

        val result = overridableMember.transform(copier, null) as IrOverridableMember

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
            arguments.memoryOptimizedMap { argument ->
                (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                    ?: argument
            }

        private fun IrType.mergeTypeAnnotations(other: IrType): IrType {
            // Flexible types are represented as a type annotation in IR, so we need to keep it when substituting type during override.
            // Note that it's incorrect to merge _all_ type annotations though, because for a Collection subclass:
            //
            //     abstract class Z : Collection<Int>
            //
            // `Z.contains` should have the signature `(Int) -> Boolean`, NOT `(@UnsafeVariance Int) -> Boolean` which would occur if we
            // copied all type annotations.
            return addAnnotations(buildList {
                for (fqName in TYPE_ANNOTATIONS_TO_MERGE) {
                    addIfNotNull(other.annotations.findAnnotation(fqName))
                }
            })
        }

        override fun remapType(type: IrType): IrType {
            if (type !is IrSimpleType) return type

            return when (val substitutedType = typeArguments[type.classifier]) {
                is IrDynamicType -> substitutedType
                is IrSimpleType -> substitutedType.mergeNullability(type).mergeTypeAnnotations(type)
                else -> type.buildSimpleType {
                    kotlinType = null
                    classifier = symbolRemapper.getReferencedClassifier(type.classifier)
                    arguments = remapTypeArguments(type.arguments)
                    annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
                }
            }
        }
    }

    private class FakeOverrideSymbolRemapperImpl(
        private val typeArguments: Map<IrTypeParameterSymbol, IrType>,
        descriptorsRemapper: DescriptorsRemapper
    ) : DeepCopySymbolRemapper(descriptorsRemapper) {

        // We need to avoid visiting parts of the tree that would not be visited by [FakeOverrideCopier]
        // Otherwise, we can record symbols inside them for rebinding, but would not actually copy them.
        // Normally, this is not important, as these local symbols would not be used anyway
        // But they can be used in return values of functions/accessors if it is effectively a private declaration,
        // e.g. public declaration inside a private class.

        override fun visitField(declaration: IrField) {} // don't visit property backing field
        override fun visitBlockBody(body: IrBlockBody) {} // don't visit function body and parameter default values
        override fun visitExpressionBody(body: IrExpressionBody) {} // don't visit function body and parameter default values

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments[result]?.classifierOrNull ?: result
        }
    }

    private companion object {
        // TODO (KT-64715): RawTypeAnnotation, FlexibleMutability, RawType, FlexibleArrayElementVariance?
        val TYPE_ANNOTATIONS_TO_MERGE = listOf(
            FlexibleNullability.asSingleFqName(),
            EnhancedNullability.asSingleFqName(),
        )
    }
}
