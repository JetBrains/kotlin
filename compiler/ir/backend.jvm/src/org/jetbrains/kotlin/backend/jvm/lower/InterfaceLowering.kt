/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyBodyToStatic
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.hasJvmDefault
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This phase moves interface members with default implementations to the
 * associated companion DefaultImpls with bridges, as appropriate. It then
 * performs a traversal of any other code in this interface and redirects calls
 * to the interface to the companion, if functions were moved completely.
 */
internal class InterfaceLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    private val removedFunctions = hashMapOf<IrFunctionSymbol, IrFunctionSymbol>()

    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) return

        when (irClass.kind) {
            ClassKind.INTERFACE -> handleInterface(irClass)
            ClassKind.ANNOTATION_CLASS -> handleAnnotationClass(irClass)
            else -> return
        }

        irClass.declarations.removeAll {
            it is IrFunction && removedFunctions.containsKey(it.symbol)
        }

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)
        if (defaultImplsIrClass.declarations.isNotEmpty()) {
            irClass.declarations.add(defaultImplsIrClass)
        }

        // Update IrElements (e.g., IrCalls) to point to the new functions.
        irClass.transformChildrenVoid(this)
    }

    private fun handleInterface(irClass: IrClass) {
        // There are 6 cases for functions on interfaces:
        loop@ for (function in irClass.functions) {
            when {
                /**
                 * 1) They are plain abstract interface functions, in which case we leave them:
                 */
                function.modality == Modality.ABSTRACT ->
                    continue@loop

                /**
                 * 2) They inherit a default implementation from an interface this interface
                 *    extends: create a bridge from companion to companion, unless
                 *    - the implementation is private or belongs to java.lang.Object
                 *    - we're in JVM Compatibility Default mode, in which case we go via
                 *      accessors on the parent class rather than the DefaultImpls
                 *    - we're in JVM Default mode, and we have that default implementation,
                 *      in which case we simply leave it.
                 *
                 *    ```
                 *    interface A { fun foo() = 0 }
                 *    interface B : A { }
                 *    ```
                 *
                 *    yields
                 *
                 *    ```
                 *    interface A { fun foo(); class DefaultImpls { fun foo() = 0 } }  // !! by Case 4 !!
                 *    interface B : A { class DefaultImpls { fun foo() = A.DefaultImpls.foo() } }
                 *    ```
                 */
                function.origin == IrDeclarationOrigin.FAKE_OVERRIDE -> {
                    val implementation = function.resolveFakeOverride()!!

                    when {
                        Visibilities.isPrivate(implementation.visibility) || implementation.isMethodOfAny() ->
                            continue@loop
                        context.state.jvmDefaultMode.isCompatibility -> {
                            val defaultImpl = createDefaultImpl(function)
                            defaultImpl.bridgeViaAccessorTo(function)
                        }
                        !implementation.hasJvmDefault() -> {
                            val defaultImpl = createDefaultImpl(function)
                            context.declarationFactory.getDefaultImplsFunction(implementation).also {
                                defaultImpl.bridgeToStatic(it)
                            }
                        }
                        // else -> Do nothing.
                    }
                }

                /**
                 * 3) Private methods (without @JvmDefault), default parameter dispatchers (without @JvmDefault)
                 *    and $annotation methods are always moved without bridges
                 */
                (Visibilities.isPrivate(function.visibility) && !function.hasJvmDefault())
                        || (function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER && !function.hasJvmDefault())
                        || function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS -> {
                    val defaultImpl = createDefaultImpl(function)
                    function.copyImplementationTo(defaultImpl)
                    removedFunctions[function.symbol] = defaultImpl.symbol
                }

                /**
                 * 4) _Without_ @JvmDefault, the default implementation is moved to DefaultImpls and
                 *    an abstract stub is left.
                 */
                !function.hasJvmDefault() -> {
                    val defaultImpl = createDefaultImpl(function)
                    function.copyImplementationTo(defaultImpl)
                    function.body = null
                    //TODO reset modality to abstract
                }

                /**
                 * 5) _With_ @JvmDefault, we move and bridge if in compatibility mode, ...
                 */
                context.state.jvmDefaultMode.isCompatibility -> {
                    val defaultImpl = createDefaultImpl(function)
                    defaultImpl.bridgeViaAccessorTo(function)
                }

                // 6) ... otherwise we simply leave the default function implementation on the interface.
            }
        }

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)

        // Move metadata for local delegated properties from the interface to DefaultImpls, since this is where kotlin-reflect looks for it.
        val localDelegatedProperties = context.localDelegatedProperties[irClass.attributeOwnerId as IrClass]
        if (localDelegatedProperties != null) {
            context.localDelegatedProperties[defaultImplsIrClass.attributeOwnerId as IrClass] = localDelegatedProperties
            context.localDelegatedProperties[irClass.attributeOwnerId as IrClass] = emptyList<IrLocalDelegatedPropertySymbol>()
        }

        // Move $$delegatedProperties array
        val delegatedPropertyArray = irClass.declarations.filterIsInstance<IrField>()
            .singleOrNull { it.origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE }
        if (delegatedPropertyArray != null) {
            irClass.declarations.remove(delegatedPropertyArray)
            defaultImplsIrClass.declarations.add(0, delegatedPropertyArray)
            delegatedPropertyArray.parent = defaultImplsIrClass
            delegatedPropertyArray.initializer?.patchDeclarationParents(defaultImplsIrClass)
        }
    }

    private fun handleAnnotationClass(irClass: IrClass) {
        // We produce $DefaultImpls for annotation classes only to move $annotations methods (for property annotations) there.
        val annotationsMethods =
            irClass.functions.filter { it.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS }
        if (annotationsMethods.none()) return

        for (function in annotationsMethods) {
            removedFunctions[function.symbol] = createDefaultImpl(function).symbol
        }
    }

    private fun createDefaultImpl(function: IrSimpleFunction): IrSimpleFunction =
        context.declarationFactory.getDefaultImplsFunction(function).also { newFunction ->
            newFunction.body = function.body?.patchDeclarationParents(newFunction)
            newFunction.parentAsClass.declarations.add(newFunction)
        }

    private fun IrSimpleFunction.copyImplementationTo(target: IrSimpleFunction) {
        copyBodyToStatic(this, target)
    }

    // Bridge from static to static method - simply fill the arguments to the parameters.
    // By nature of the generation of both source and target of bridge, they line up.
    private fun IrFunction.bridgeToStatic(callTarget: IrFunction) {
        body = IrExpressionBodyImpl(IrCallImpl(startOffset, endOffset, returnType, callTarget.symbol).also { call ->
            call.passTypeArgumentsFrom(this)
            valueParameters.forEachIndexed { i, it ->
                call.putValueArgument(i, IrGetValueImpl(startOffset, endOffset, it.symbol))
            }
        })
    }

    // Bridge from static DefaultImpl method to the interface method. Arguments need to
    // be shifted in presence of dispatch and extension receiver.
    private fun IrFunction.bridgeViaAccessorTo(callTarget: IrFunction) {
        body = IrExpressionBodyImpl(
            IrCallImpl(
                startOffset,
                endOffset,
                returnType,
                callTarget.symbol,
                superQualifierSymbol = callTarget.parentAsClass.symbol
            ).also { call ->
                call.passTypeArgumentsFrom(this)

                var offset = 0
                callTarget.dispatchReceiverParameter?.let {
                    call.dispatchReceiver = IrGetValueImpl(startOffset, endOffset, valueParameters[offset].symbol)
                    offset += 1
                }
                callTarget.extensionReceiverParameter?.let {
                    call.extensionReceiver = IrGetValueImpl(startOffset, endOffset, valueParameters[offset].symbol)
                    offset += 1
                }
                for (i in offset until valueParameters.size) {
                    call.putValueArgument(i - 1, IrGetValueImpl(startOffset, endOffset, valueParameters[i].symbol))
                }
            })
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
