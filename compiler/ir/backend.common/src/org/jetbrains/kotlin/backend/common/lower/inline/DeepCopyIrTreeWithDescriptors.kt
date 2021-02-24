/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

internal class DeepCopyIrTreeWithSymbolsForInliner(
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
    val parent: IrDeclarationParent?
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

    private var nameIndex = 0

    private fun generateCopyName(name: Name) = Name.identifier(name.toString() + "_" + (nameIndex++).toString())

    private inner class InlinerSymbolRenamer : SymbolRenamer {
        private val map = mutableMapOf<IrSymbol, Name>()

        override fun getClassName(symbol: IrClassSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    private inner class InlinerTypeRemapper(
        val symbolRemapper: SymbolRemapper,
        val typeArguments: Map<IrTypeParameterSymbol, IrType?>?
    ) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

        override fun leaveScope() {}

        private fun remapTypeArguments(arguments: List<IrTypeArgument>, erase: Boolean) =
            arguments.map { argument ->
                (argument as? IrTypeProjection)?.let { makeTypeProjection(remapTypeAndOptionallyErase(it.type, erase), it.variance) }
                    ?: argument
            }

        override fun remapType(type: IrType) = remapTypeAndOptionallyErase(type, erase = false)

        fun remapTypeAndOptionallyErase(type: IrType, erase: Boolean): IrType {
            if (type !is IrSimpleType) return type

            val classifier = type.classifier
            val substitutedType = typeArguments?.get(classifier)

            // Erase non-reified type parameter if asked to.
            if (erase && substitutedType != null && (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false) {
                // Pick the (necessarily unique) non-interface upper bound if it exists.
                val superClass = classifier.owner.superTypes.firstOrNull {
                    it.classOrNull?.owner?.isInterface == false
                }
                val erasedUpperBound = superClass
                    ?: remapTypeAndOptionallyErase(classifier.owner.superTypes.first(), erase = true)

                return if (type.hasQuestionMark) erasedUpperBound.makeNullable() else erasedUpperBound
            }

            if (substitutedType is IrDynamicType) return substitutedType

            if (substitutedType is IrSimpleType) {
                return substitutedType.buildSimpleType {
                    kotlinType = null
                    hasQuestionMark = type.hasQuestionMark or substitutedType.isMarkedNullable()
                }
            }

            return type.buildSimpleType {
                kotlinType = null
                this.classifier = symbolRemapper.getReferencedClassifier(classifier)
                arguments = remapTypeArguments(type.arguments, erase)
                annotations = type.annotations.map { it.transform(copier, null) as IrConstructorCall }
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
    private val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments)
    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, InlinerSymbolRenamer()) {
        private fun IrType.remapTypeAndErase() = typeRemapper.remapTypeAndOptionallyErase(this, erase = true)

        override fun visitTypeOperator(expression: IrTypeOperatorCall) =
            IrTypeOperatorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapTypeAndErase(),
                expression.operator,
                expression.typeOperand.remapTypeAndErase(),
                expression.argument.transform()
            ).copyAttributes(expression)
    }
}
