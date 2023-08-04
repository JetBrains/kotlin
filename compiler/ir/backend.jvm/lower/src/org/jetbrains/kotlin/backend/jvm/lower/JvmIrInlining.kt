/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.inline.AbstractDeepCopyIrTreeWithSymbolsForInliner
import org.jetbrains.kotlin.backend.common.lower.inline.InlinerTypeRemapper
import org.jetbrains.kotlin.backend.common.lower.inline.IrTreeWithSymbolsCopier
import org.jetbrains.kotlin.backend.common.lower.inline.NonReifiedTypeParameterRemappingMode
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.withinScope
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class JvmDeepCopyIrTreeWithSymbolsForInliner(
    context: JvmBackendContext,
    typeArguments: Map<IrTypeParameterSymbol, IrType?>?, parent: IrDeclarationParent?,
) : AbstractDeepCopyIrTreeWithSymbolsForInliner(typeArguments, parent) {

    override val copier: JvmIrTreeWithSymbolsCopier

    init {
        val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments, NonReifiedTypeParameterRemappingMode.ERASE)
        copier = JvmIrTreeWithSymbolsCopier(context, symbolRemapper, typeRemapper)
        typeRemapper.copier = copier
    }
}

class JvmIrTreeWithSymbolsCopier(
    val context: JvmBackendContext,
    symbolRemapper: SymbolRemapper,
    override val typeRemapper: InlinerTypeRemapper,
) : IrTreeWithSymbolsCopier(symbolRemapper, typeRemapper) {

    private var currentElement: IrElement? = null

    override fun handleReification() {
        currentElement?.also { context.elementsWithReifiedTypes.add(it) }
    }

    private fun handleTransformation(original: IrElement, transformed: IrElement) {
        if (original in context.elementsWithReifiedTypes) {
            context.elementsWithReifiedTypes.add(transformed)
        }
        currentElement = transformed
    }

    override fun visitClass(declaration: IrClass): IrClass {
        return declaration.factory.createClass(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = declaration.name,
            visibility = declaration.visibility,
            symbol = symbolRemapper.getDeclaredClass(declaration.symbol),
            kind = declaration.kind,
            modality = declaration.modality,
            isExternal = declaration.isExternal,
            isCompanion = declaration.isCompanion,
            isInner = declaration.isInner,
            isData = declaration.isData,
            isValue = declaration.isValue,
            isExpect = declaration.isExpect,
            isFun = declaration.isFun,
        ).apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            handleTransformation(declaration, this)
            superTypes = declaration.superTypes.memoryOptimizedMap {
                it.substituteAll(true)
            }
            sealedSubclasses = declaration.sealedSubclasses.memoryOptimizedMap {
                symbolRemapper.getReferencedClass(it)
            }
            // Consider the code:
            //
            // `inline fun <reified T> foo() { object {} }`
            //
            // Here `object {}` is implicitly parametrized by `T` (KT-9584), and so `thisReceiver` is.
            // Yet different parameterizations of `foo` produces the same bytecode for `object {}`, so we should ignore reification here
            // See kt60906.kt for an example
            thisReceiver = transformValueParameter(declaration.thisReceiver, true)
            valueClassRepresentation = declaration.valueClassRepresentation?.mapUnderlyingType { it.remapType() as IrSimpleType }
            declaration.transformDeclarationsTo(this)
        }.copyAttributes(declaration)
    }

    private fun transformValueParameter(declaration: IrValueParameter?, ignoreReification: Boolean): IrValueParameter? {
        if (declaration == null) return null
        return declaration.factory.createValueParameter(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = declaration.name,
            type = declaration.type.erase(!ignoreReification),
            isAssignable = declaration.isAssignable,
            symbol = symbolRemapper.getDeclaredValueParameter(declaration.symbol),
            index = declaration.index,
            varargElementType = declaration.varargElementType?.erase(),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            transformAnnotations(declaration)
            defaultValue = declaration.defaultValue?.transform()
        }
    }

    override fun visitCall(expression: IrCall): IrCall {
        return super.visitCall(expression).apply {
            handleTransformation(expression, this)
            for (i in 0 until typeArgumentsCount) {
                val remappedTypeArg = if (Symbols.isTypeOfIntrinsic(expression.symbol)) {
                    expression.getTypeArgument(i)?.leaveNonReifiedAsIs(true)
                } else {
                    expression.getTypeArgument(i)?.erase(true)
                }
                putTypeArgument(i, remappedTypeArg)
            }
        }
    }

    override fun visitClassReference(expression: IrClassReference): IrClassReference {
        return super.visitClassReference(expression).apply {
            handleTransformation(expression, this)
            classType = expression.classType.erase(true)
        }
    }

    override fun visitField(declaration: IrField): IrField {
        return super.visitField(declaration).apply {
            handleTransformation(declaration, this)
            type = declaration.type.erase(true)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCallImpl {
        return super.visitTypeOperator(expression).apply {
            handleTransformation(expression, this)
            typeOperand = expression.typeOperand.erase(true)
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter {
        return transformValueParameter(declaration, false)!!
    }

    override fun visitVariable(declaration: IrVariable): IrVariable {
        return super.visitVariable(declaration).apply {
            handleTransformation(declaration, this)
            type = declaration.type.erase(true)
        }
    }

    override fun <T : IrFunction> T.transformFunctionChildren(declaration: T): T =
        apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            typeRemapper.withinScope(this) {
                // Ignoring reification here has the same rationale as ignoring `thisReceiver` when transforming classes
                dispatchReceiverParameter = transformValueParameter(declaration.dispatchReceiverParameter, true)
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                returnType = typeRemapper.remapType(declaration.returnType)
                valueParameters = declaration.valueParameters.transform()
                body = declaration.body?.transform()
            }
        }
}