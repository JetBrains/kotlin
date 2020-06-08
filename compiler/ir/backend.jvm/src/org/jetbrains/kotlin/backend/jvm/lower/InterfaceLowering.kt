/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.descriptors.ClassKind
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
import org.jetbrains.kotlin.ir.types.defaultType
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
        val jvmDefaultMode = context.state.jvmDefaultMode
        val isCompatibilityMode = jvmDefaultMode.isCompatibility && !irClass.hasJvmDefaultNoCompatibilityAnnotation()
        // There are 6 cases for functions on interfaces:
        loop@ for (function in irClass.functions) {
            when {
                /**
                 * 1) They are plain abstract interface functions, in which case we leave them:
                 */
                function.modality == Modality.ABSTRACT ->
                    continue@loop

                /**
                 * 2) They inherit a default implementation from an interface this interface extends:
                 *    create a bridge from DefaultImpls of derived to DefaultImpls of base, unless
                 *    - the implementation is private, or belongs to java.lang.Object,
                 *      or is a stub for function with default parameters ($default)
                 *    - we're in -Xjvm-default=compatibility|all-compatibility mode, in which case we go via
                 *      accessors on the parent class rather than the DefaultImpls if inherited method is compiled to JVM default
                 *    - we're in -Xjvm-default=enable|all mode, and we have that default implementation,
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
                function.isFakeOverride -> {
                    // We check to see if this is a default stub function BEFORE finding the implementation because of a front-end bug
                    // (KT-36188) where there could be multiple implementations. (resolveFakeOverride() only returns the implementation if
                    // there's only one.)
                    if (function.name.asString().endsWith("\$default")) {
                        continue@loop
                    }
                    val implementation = function.resolveFakeOverride() ?: error("No single implementation found for: ${function.render()}")

                    when {
                        Visibilities.isPrivate(implementation.visibility) || implementation.isMethodOfAny() ->
                            continue@loop
                        !function.isDefinitelyNotDefaultImplsMethod(jvmDefaultMode, implementation) -> {
                            val defaultImpl = createDefaultImpl(function)
                            val superImpl = firstSuperMethodFromKotlin(function, implementation)
                            context.declarationFactory.getDefaultImplsFunction(superImpl.owner).also {
                                defaultImpl.bridgeToStatic(it)
                            }
                        }
                        isCompatibilityMode && implementation.isCompiledToJvmDefault(jvmDefaultMode) -> {
                            val defaultImpl = createDefaultImpl(function)
                            defaultImpl.bridgeViaAccessorTo(function)
                        }
                    }
                }

                /**
                 * 3) Private methods (not compiled to JVM defaults), default parameter dispatchers (not compiled to JVM defaults)
                 *    and $annotation methods are always moved without bridges
                 */
                (Visibilities.isPrivate(function.visibility) && !function.isCompiledToJvmDefault(jvmDefaultMode))
                        || (function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER && !function.isCompiledToJvmDefault(jvmDefaultMode))
                        || function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS -> {
                    val defaultImpl = createDefaultImpl(function)
                    defaultImpl.body = function.moveBodyTo(defaultImpl)
                    removedFunctions[function.symbol] = defaultImpl.symbol
                }

                /**
                 * 4) Non JVM default implementation with body is moved to DefaultImpls and
                 *    an abstract stub is left.
                 */
                !function.isCompiledToJvmDefault(jvmDefaultMode) -> {
                    val defaultImpl = createDefaultImpl(function)
                    defaultImpl.body = function.moveBodyTo(defaultImpl)
                    function.body = null
                    //TODO reset modality to abstract
                }

                /**
                 * 5) JVM default declaration is bridged in DefaultImpls via accessor if in compatibility mode, ...
                 */
                isCompatibilityMode -> {
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

        // Move $$delegatedProperties array and $assertionsDisabled field
        for (field in irClass.declarations.filterIsInstance<IrField>()) {
            if (field.origin != JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE && field.origin != JvmLoweredDeclarationOrigin.GENERATED_ASSERTION_ENABLED_FIELD)
                continue

            irClass.declarations.remove(field)
            defaultImplsIrClass.declarations.add(0, field)
            field.parent = defaultImplsIrClass
            field.initializer?.patchDeclarationParents(defaultImplsIrClass)
        }
    }

    private fun handleAnnotationClass(irClass: IrClass) {
        // We produce $DefaultImpls for annotation classes only to move $annotations methods (for property annotations) there.
        val annotationsMethods =
            irClass.functions.filter { it.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS }
        if (annotationsMethods.none()) return

        for (function in annotationsMethods) {
            val defaultImpl = createDefaultImpl(function)
            defaultImpl.body = function.moveBodyTo(defaultImpl)
            removedFunctions[function.symbol] = defaultImpl.symbol
        }
    }

    private fun createDefaultImpl(function: IrSimpleFunction): IrSimpleFunction =
        context.declarationFactory.getDefaultImplsFunction(function).also { newFunction ->
            newFunction.parentAsClass.declarations.add(newFunction)
        }

    // Bridge from static to static method - simply fill the function arguments to the parameters.
    // By nature of the generation of both source and target of bridge, they line up.
    private fun IrFunction.bridgeToStatic(callTarget: IrFunction) {
        body = IrExpressionBodyImpl(IrCallImpl(startOffset, endOffset, returnType, callTarget.symbol).also { call ->

            callTarget.typeParameters.forEachIndexed { i, _ ->
                call.putTypeArgument(i, createPlaceholderAnyNType(context.irBuiltIns))
            }

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
                this.typeParameters.drop(callTarget.parentAsClass.typeParameters.size).forEachIndexed { i, typeParameter ->
                    call.putTypeArgument(i, typeParameter.defaultType)
                }

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
                    call.putValueArgument(i - offset, IrGetValueImpl(startOffset, endOffset, valueParameters[i].symbol))
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
                createDelegatingCallWithPlaceholderTypeArguments(expression, newFunction, context.irBuiltIns)
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
                        startOffset, endOffset, type, newFunction.symbol, newFunction.typeParameters.size, newFunction.valueParameters.size,
                        expression.reflectionTarget, origin
                    ).apply {
                        copyFromWithPlaceholderTypeArguments(expression, context.irBuiltIns)
                        copyAttributes(expression)
                    }
                }
            } else {
                expression
            }
        )
    }
}
