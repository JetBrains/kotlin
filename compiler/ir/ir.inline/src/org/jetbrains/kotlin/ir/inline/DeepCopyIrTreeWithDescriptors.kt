/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class DeepCopyIrTreeWithSymbolsForInliner(
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
    val parent: IrDeclarationParent?,
    defaultNonReifiedTypeParameterRemappingMode: NonReifiedTypeParameterRemappingMode,
) {

    fun copy(irElement: IrElement): IrElement {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        result.patchDeclarationParents(parent)
        return result
    }

    private inner class InlinerTypeRemapper(
        val symbolRemapper: SymbolRemapper,
        val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
        val defaultNonReifiedTypeParameterRemappingMode: NonReifiedTypeParameterRemappingMode,
    ) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

        override fun leaveScope() {}

        private fun remapTypeArguments(
            arguments: List<IrTypeArgument>,
            erasedParameters: MutableSet<IrTypeParameterSymbol>?,
            leaveNonReifiedAsIs: Boolean,
        ) =
            arguments.memoryOptimizedMap { argument ->
                (argument as? IrTypeProjection)?.let { proj ->
                    remapType(proj.type, erasedParameters, leaveNonReifiedAsIs)?.let { newType ->
                        makeTypeProjection(newType, proj.variance)
                    } ?: IrStarProjectionImpl
                }
                    ?: argument
            }

        override fun remapType(type: IrType) = remapType(type, defaultNonReifiedTypeParameterRemappingMode)

        fun remapType(type: IrType, mode: NonReifiedTypeParameterRemappingMode): IrType {
            val erasedParams = if (mode == NonReifiedTypeParameterRemappingMode.ERASE) mutableSetOf<IrTypeParameterSymbol>() else null
            return remapType(type, erasedParams, mode == NonReifiedTypeParameterRemappingMode.LEAVE_AS_IS)
                ?: error("Cannot substitute type ${type.render()}")
        }

        private fun remapType(type: IrType, erasedParameters: MutableSet<IrTypeParameterSymbol>?, leaveNonReifiedAsIs: Boolean): IrType? {
            if (type !is IrSimpleType) return type

            val classifier = type.classifier
            val substitutedType = typeArguments?.get(classifier)

            if (leaveNonReifiedAsIs && classifier is IrTypeParameterSymbol && !classifier.owner.isReified) {
                return type
            }

            // Erase non-reified type parameter if asked to.
            if (erasedParameters != null && substitutedType != null && (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false) {

                if (classifier in erasedParameters) {
                    return null
                }

                erasedParameters.add(classifier)

                // Pick the (necessarily unique) non-interface upper bound if it exists.
                val superTypes = classifier.owner.superTypes
                val superClass = superTypes.firstOrNull {
                    it.classOrNull?.owner?.isInterface == false
                }

                val upperBound = superClass ?: superTypes.first()

                // TODO: Think about how to reduce complexity from k^N to N^k
                val erasedUpperBound = remapType(upperBound, erasedParameters, leaveNonReifiedAsIs)
                    ?: error("Cannot erase upperbound ${upperBound.render()}")

                erasedParameters.remove(classifier)

                return erasedUpperBound.mergeNullability(type)
            }

            if (substitutedType is IrDynamicType) return substitutedType

            if (substitutedType is IrSimpleType) {
                return substitutedType.mergeNullability(type)
            }

            return type.buildSimpleType {
                kotlinType = null
                this.classifier = symbolRemapper.getReferencedClassifier(classifier)
                arguments = remapTypeArguments(type.arguments, erasedParameters, leaveNonReifiedAsIs)
                annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
            }
        }
    }

    private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper) : DeepCopySymbolRemapper(descriptorsRemapper) {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper = SymbolRemapperImpl(NullDescriptorsRemapper)
    private val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments, defaultNonReifiedTypeParameterRemappingMode)
    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {

        private fun IrType.leaveNonReifiedAsIs() = typeRemapper.remapType(this, NonReifiedTypeParameterRemappingMode.LEAVE_AS_IS)

        private fun IrType.substituteAll() = typeRemapper.remapType(this, NonReifiedTypeParameterRemappingMode.SUBSTITUTE)

        private fun IrType.erase() = typeRemapper.remapType(this, NonReifiedTypeParameterRemappingMode.ERASE)

        override fun visitClass(declaration: IrClass): IrClass {
            // Substitute type argument to make Class::genericSuperclass work as expected (see kt52417.kt)
            // Substitution to the super types does not lead to reification and therefore is safe
            return super.visitClass(declaration).apply {
                superTypes = declaration.superTypes.memoryOptimizedMap {
                    it.substituteAll()
                }
            }
        }

        override fun visitCall(expression: IrCall): IrCall {
            if (!Symbols.isTypeOfIntrinsic(expression.symbol)) return super.visitCall(expression)
            // We should neither erase nor substitute non-reified type parameters in the `typeOf` call so that reflection is able
            // to create a proper KTypeParameter for it. See KT-60175, KT-30279.
            return IrCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type,
                expression.symbol,
                expression.typeArgumentsCount,
                expression.valueArgumentsCount,
                expression.origin,
                expression.superQualifierSymbol
            ).apply {
                for (i in 0 until typeArgumentsCount) {
                    putTypeArgument(i, expression.getTypeArgument(i)?.leaveNonReifiedAsIs())
                }
            }.copyAttributes(expression)
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall) =
            IrTypeOperatorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.erase(),
                expression.operator,
                expression.typeOperand.erase(),
                expression.argument.transform()
            ).copyAttributes(expression)
    }
}
