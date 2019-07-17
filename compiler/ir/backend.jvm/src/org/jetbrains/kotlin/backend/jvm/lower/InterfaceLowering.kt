/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering.Companion.clinitName
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.hasJvmDefault
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val interfacePhase = makeIrFilePhase(
    ::InterfaceLowering,
    name = "Interface",
    description = "Move default implementations of interface members to DefaultImpls class"
)

private class InterfaceLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state = context.state
    val removedFunctions = hashMapOf<IrFunctionSymbol, IrFunctionSymbol>()

    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)
        irClass.declarations.add(defaultImplsIrClass)
        val members = defaultImplsIrClass.declarations

        for (function in irClass.declarations) {
            if (function !is IrSimpleFunction) continue

            if (function.modality != Modality.ABSTRACT && function.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                val element = context.declarationFactory.getDefaultImplsFunction(function).also {
                    if (shouldRemoveFunction(function))
                        removedFunctions[function.symbol] = it.symbol
                }
                members.add(element)
                element.body = function.body?.patchDeclarationParents(element)
                if (function.hasJvmDefault() &&
                    function.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS
                ) {
                    // TODO: don't touch function and only generate element / DefaultImpls when needed.
                    function.body = IrExpressionBodyImpl(callDefaultImpls(element, function))
                } else {
                    function.body = null
                    //TODO reset modality to abstract
                }
            }
        }

        // Update IrElements (e.g., IrCalls) to point to the new functions.
        irClass.transformChildrenVoid(this)

        irClass.declarations.removeAll {
            it is IrFunction && removedFunctions.containsKey(it.symbol)
        }

        // Move metadata for local delegated properties from the interface to DefaultImpls, since this is where kotlin-reflect looks for it.
        val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId as IrClass]
        if (localDelegatedProperties != null) {
            context.localDelegatedProperties[defaultImplsIrClass.attributeOwnerId as IrClass] = localDelegatedProperties
            context.localDelegatedProperties[irClass.attributeOwnerId as IrClass] = emptyList<IrLocalDelegatedPropertySymbol>()
        }
    }

    private fun shouldRemoveFunction(function: IrFunction): Boolean =
        Visibilities.isPrivate(function.visibility) && function.name != clinitName ||
                function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS

    private fun callDefaultImpls(defaultImpls: IrFunction, interfaceMethod: IrFunction): IrCall {
        val startOffset = interfaceMethod.startOffset
        val endOffset = interfaceMethod.endOffset

        return IrCallImpl(startOffset, endOffset, interfaceMethod.returnType, defaultImpls.symbol).apply {
            passTypeArgumentsFrom(interfaceMethod)

            var offset = 0
            interfaceMethod.dispatchReceiverParameter?.let {
                putValueArgument(offset++, IrGetValueImpl(startOffset, endOffset, it.symbol))
            }
            interfaceMethod.extensionReceiverParameter?.let {
                putValueArgument(offset++, IrGetValueImpl(startOffset, endOffset, it.symbol))
            }
            interfaceMethod.valueParameters.forEachIndexed { i, it ->
                putValueArgument(i + offset, IrGetValueImpl(startOffset, endOffset, it.symbol))
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        val newFunction = removedFunctions[expression.returnTargetSymbol]?.owner
        return super.visitReturn(
            if (newFunction != null) {
                with(expression) {
                    IrReturnImpl(startOffset, endOffset, type, newFunction.symbol, value)
                }
            } else {
                expression
            }
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val newFunction = removedFunctions[expression.symbol]?.owner
        return super.visitCall(
            if (newFunction != null) {
                irCall(expression, newFunction, receiversAsArguments = true)
            } else {
                expression
            }
        )
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val newFunction = removedFunctions[expression.symbol]?.owner
        return super.visitFunctionReference(
            if (newFunction != null) {
                with(expression) {
                    IrFunctionReferenceImpl(
                        startOffset,
                        endOffset,
                        type,
                        newFunction.symbol,
                        newFunction.descriptor,
                        typeArgumentsCount,
                        origin
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression, receiversAsArguments = true)
                        copyAttributes(expression)
                    }
                }
            } else {
                expression
            }
        )
    }
}
