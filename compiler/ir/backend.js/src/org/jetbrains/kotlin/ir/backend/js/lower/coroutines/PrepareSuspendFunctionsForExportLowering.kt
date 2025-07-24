/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering.Companion.bridgeFunction
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.config.compileLambdasAsEs6ArrowFunctions
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * The lowering generates a few functions from the original exported suspend functions
 * to be used both from Kotlin and JS sides with minimal performance overhead.
 *
 * It works both with top-level suspend functions and with member suspend functions.
 *
 * For the top-level suspend functions we generate only one additional function supposed to be used only from the JS side.
 * As an example:
 *
 * **Before the transformation:**
 * ```kotlin
 * @JsExport
 * suspend fun foo(a: Int, b: String): String { ... }
 * ```
 *
 * **After the transformation:**
 * ```kotlin
 * @JsExport.Ignore
 * suspend fun foo(a: Int, b: String): String { ... }
 *
 * @JsExport
 * @JsName("foo")
 * fun foo$promisified(a: Int, b: String): Promise<String> =
 *     kotlin.coroutines.promisify { foo(a, b) }
 * ```
 *
 * For member functions it's a little bit more complicated because we have to take into account that they could be overridden from the JS side.
 * To support the overriding, we introduce two more helper methods.
 *
 * **Before the transformation:**
 * ```kotlin
 * @JsExport
 * open class SomeClass {
 *   open suspend fun foo(a: Int, b: String): String { ... }
 * }
 * ```
 *
 * **After the transformation:**
 * ```kotlin
 * @JsExport
 * open class SomeClass {
 *   @JsName("foo")
 *   // This function is used from the JS side and is supposed to be overridden from the JS side
 *   fun foo$promisified(a: Int, b: String): Promise<String> =
 *       kotlin.coroutines.promisify { this.foo$original(a, b) }
 *
 *   @JsExport.Ignore
 *   // This function is supposed to be overridden from the Kotlin side
 *   suspend fun foo(a: Int, b: String): String { ... }
 *
 *   @JsExport.Ignore
 *   // This function is used on the call side
 *   open suspend fun foo$suspendBridge(a: Int, b: String): String =
 *     if (this.foo === SomeClass.prototype.foo) {
 *       this.foo$original(a, b)
 *     } else {
 *       kotlin.coroutines.await(this.foo$promisified(a, b))
 *     }
 * }
 * ```
 *
 * Such a trick with the 3 functions helps us to call the original function instead of the promisified variant as long as the function
 * is not overridden from the JS side and also makes sure we don't recompile each child class if the body of the exported function
 * is changed.
 */
internal class PrepareSuspendFunctionsForExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    companion object {
        private const val PROMISIFIED_WRAPPER_SUFFIX = "\$promisified"
        private const val EXPORTED_SUSPEND_FUNCTION_BRIDGE_SUFFIX = "\$suspendBridge"

        val PROMISIFIED_WRAPPER by IrDeclarationOriginImpl
        val EXPORTED_SUSPEND_FUNCTION_BRIDGE by IrDeclarationOriginImpl
        val EXPORTED_SUSPEND_FUNCTION_BRIDGE_PARAMETER by IrDeclarationOriginImpl

        var IrFunction.bridgeFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
        var IrFunction.promisifiedWrapperFunction: IrSimpleFunction? by irAttribute(copyByDefault = true)
    }

    private val dynamicType = context.dynamicType
    private val promiseClass = context.intrinsics.promiseClassSymbol
    private val jsNameAnnotation = context.intrinsics.jsNameAnnotationSymbol.owner.constructors.single()
    private val jsExportAnnotation = context.intrinsics.jsExportAnnotationSymbol.owner.constructors.single()
    private val awaitFunctionSymbol = context.intrinsics.awaitFunctionSymbol
    private val jsClassFunctionSymbol = context.intrinsics.jsClass
    private val jsEqeqeqFunctionSymbol = context.intrinsics.jsEqeqeq
    private val promisifyFunctionSymbol = context.intrinsics.promisifyFunctionSymbol
    private val jsExportIgnoreAnnotation = context.intrinsics.jsExportIgnoreAnnotationSymbol.owner.constructors.single()
    private val jsPrototypeFunctionSymbol = context.intrinsics.jsPrototypeOfSymbol
    private val suspendFunctionClassSymbol = context.irBuiltIns.suspendFunctionN(0).symbol

    private val IrOverridableDeclaration<*>.isInterfaceMethod: Boolean
        get() = parentClassOrNull?.isInterface == true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (
            declaration !is IrSimpleFunction ||
            declaration.isFakeOverride ||
            !declaration.isExportedSuspendFunction(context)
        ) return null

        val isTheFirstRealOverriddenMethod =
            (declaration.overriddenSymbols.isEmpty() || declaration.overriddenSymbols.all { it.owner.isInterfaceMethod })
                    && !declaration.isInterfaceMethod

        val promisifiedWrapperFunction = generatePromisifiedWrapper(declaration, isTheFirstRealOverriddenMethod)

        return when {
            declaration.isTopLevel || !isTheFirstRealOverriddenMethod -> listOf(promisifiedWrapperFunction, declaration)
            else -> {
                val bridgeFunction = generateBridgeFunction(declaration, promisifiedWrapperFunction)

                declaration.bridgeFunction = bridgeFunction
                declaration.promisifiedWrapperFunction = promisifiedWrapperFunction

                listOf(promisifiedWrapperFunction, bridgeFunction, declaration)
            }
        }
    }

    private fun generateBridgeFunction(originalFunc: IrSimpleFunction, promisifiedWrapperFunction: IrSimpleFunction): IrSimpleFunction {
        return context.irFactory.buildFun {
            updateFrom(originalFunc)
            name = Name.identifier("${originalFunc.name}${EXPORTED_SUSPEND_FUNCTION_BRIDGE_SUFFIX}")
            origin = EXPORTED_SUSPEND_FUNCTION_BRIDGE
            returnType = originalFunc.returnType
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
        }.also { bridgeFunc ->
            bridgeFunc.copyValueAndTypeParametersFrom(originalFunc)

            bridgeFunc.parent = originalFunc.parent
            bridgeFunc.returnType = originalFunc.returnType.remapTypeParameters(originalFunc, bridgeFunc)

            bridgeFunc.annotations = buildList {
                add(JsIrBuilder.buildConstructorCall(jsExportIgnoreAnnotation.symbol))
                originalFunc.annotations.filterTo(this) { !it.isAnnotation(JsAnnotations.jsNameFqn) }
            }.compactIfPossible()

            bridgeFunc.parameters.forEach {
                it.defaultValue = null
                it.origin = EXPORTED_SUSPEND_FUNCTION_BRIDGE_PARAMETER
            }

            val originalName =
                promisifiedWrapperFunction.getJsName() ?: compilationException(
                    "Promisified wrapper function should contain at least one @JsName annotation",
                    promisifiedWrapperFunction
                )

            val dispatchReceiverParameter =
                bridgeFunc.dispatchReceiverParameter ?: compilationException(
                    "This function should be applied only to a member function",
                    bridgeFunc
                )

            bridgeFunc.body = context.createIrBuilder(bridgeFunc.symbol).irBlockBody(bridgeFunc) {
                val isExportedFunctionOverridden = irCall(jsEqeqeqFunctionSymbol).apply {
                    arguments[0] = JsIrBuilder.buildDynamicMemberExpression(irGet(dispatchReceiverParameter), originalName, dynamicType)
                    arguments[1] = JsIrBuilder.buildDynamicMemberExpression(
                        irCall(jsPrototypeFunctionSymbol).apply {
                            arguments[0] = irCall(jsClassFunctionSymbol).apply {
                                typeArguments[0] = dispatchReceiverParameter.type
                            }
                        },
                        originalName,
                        dynamicType
                    )
                }
                +irReturn(
                    irIfThenElse(
                        bridgeFunc.returnType,
                        isExportedFunctionOverridden,
                        irCallWithParametersOf(originalFunc, bridgeFunc),
                        irCall(awaitFunctionSymbol).apply {
                            arguments[0] = irCallWithParametersOf(promisifiedWrapperFunction, bridgeFunc)
                        }
                    )
                )
            }
        }.patchDeclarationParents()
    }

    private fun generatePromisifiedWrapper(
        originalFunc: IrSimpleFunction,
        originalFunctionIsTheFirstRealMethod: Boolean,
    ): IrSimpleFunction {
        return context.irFactory.buildFun {
            updateFrom(originalFunc)
            name = Name.identifier("${originalFunc.name.asString()}${PROMISIFIED_WRAPPER_SUFFIX}")
            origin = PROMISIFIED_WRAPPER
            isSuspend = false
            isFakeOverride = !originalFunc.isInterfaceMethod && !originalFunctionIsTheFirstRealMethod
            modality = Modality.OPEN
            returnType =
                IrSimpleTypeImpl(promiseClass, SimpleTypeNullability.NOT_SPECIFIED, listOf(originalFunc.returnType), emptyList())
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
        }.apply {
            parent = originalFunc.parent
            copyValueAndTypeParametersFrom(originalFunc)
            parameters.forEach {
                if (it.defaultValue != null) {
                    it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
                    it.defaultValue = null
                }
            }

            val (exportAnnotations, irrelevantAnnotations) = originalFunc.annotations.partition {
                it.isAnnotation(JsAnnotations.jsExportFqn)
            }

            annotations = exportAnnotations.compactIfPossible()

            addJsName(originalFunc.getJsNameOrKotlinName().identifier)

            if (originalFunc.isTopLevel && !exportAnnotations.hasAnnotation(JsStandardClassIds.Annotations.JsExport)) {
                addJsExport()
            }

            originalFunc.annotations = irrelevantAnnotations.compactIfPossible()

            body = runIf(originalFunctionIsTheFirstRealMethod) {
                context.createIrBuilder(symbol).irBlockBody(this) {
                    val call = irCallWithParametersOf(originalFunc, this@apply)

                    if (!this@PrepareSuspendFunctionsForExportLowering.context.configuration.compileLambdasAsEs6ArrowFunctions) {
                        val selfSaver = call.dispatchReceiver?.let(::createTmpVariable)
                        call.dispatchReceiver = selfSaver?.let(::irGet)
                    }

                    val promisifiedSuspendLambda = context.irFactory.buildFun {
                        name = SpecialNames.NO_NAME_PROVIDED
                        visibility = DescriptorVisibilities.LOCAL
                        isSuspend = true
                        returnType = call.type
                    }.also {
                        it.parent = this@apply
                        it.body = irBlockBody(it) { +irReturn(call) }
                    }

                    +irReturn(
                        irCall(promisifyFunctionSymbol).apply {
                            arguments[0] = JsIrBuilder.buildFunctionExpression(
                                suspendFunctionClassSymbol.typeWith(call.type),
                                promisifiedSuspendLambda,
                            )
                        })
                }
            }
        }
    }

    private fun IrBuilderWithScope.irCallWithParametersOf(functionToCall: IrSimpleFunction, parametersSource: IrSimpleFunction): IrCall {
        return irCall(functionToCall.symbol).apply {
            parametersSource.parameters.forEachIndexed { index, irValueParameter ->
                arguments[index] = irGet(irValueParameter)
            }
            parametersSource.typeParameters.forEachIndexed { index, irTypeParameter ->
                typeArguments[index] =
                    IrSimpleTypeImpl(irTypeParameter.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
            }
        }
    }

    private fun IrMutableAnnotationContainer.addJsName(name: String) {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsNameAnnotation.symbol).apply {
            arguments[0] = name.toIrConst(context.irBuiltIns.stringType)
        }
    }

    private fun IrMutableAnnotationContainer.addJsExport() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportAnnotation.symbol)
    }
}

class ReplaceExportedSuspendFunctionsCallsWithTheirBridgeCall(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction && container.origin == PrepareSuspendFunctionsForExportLowering.EXPORTED_SUSPEND_FUNCTION_BRIDGE) return
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                // In the case of super.suspendCall as an optimization, we can keep the call without bridging it
                // since on the compile time we know that it's always a Kotlin suspend function
                if (expression.superQualifierSymbol == null && expression.symbol.owner.isExportedSuspendFunction(context)) {
                    expression.symbol.owner.bridgeFunction?.let { bridge ->
                        expression.symbol = bridge.symbol
                    }
                }

                return super.visitCall(expression)
            }
        })
    }
}

class IgnoreOriginalSuspendFunctionsThatWereExportedLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsExportIgnoreAnnotation = context.intrinsics.jsExportIgnoreAnnotationSymbol.owner.constructors.single()

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isExportedSuspendFunction(context)) {
            declaration.annotations =
                declaration.annotations.filter { !it.isAnnotation(JsAnnotations.jsNameFqn) } memoryOptimizedPlus JsIrBuilder.buildConstructorCall(
                    jsExportIgnoreAnnotation.symbol
                )
        }

        return null
    }
}

private fun IrSimpleFunction.isExportedSuspendFunction(context: JsIrBackendContext): Boolean =
    isSuspend && isExported(context)

val IrValueParameter.isProxyParameterForExportedSuspendFunction: Boolean
    get() = origin == PrepareSuspendFunctionsForExportLowering.EXPORTED_SUSPEND_FUNCTION_BRIDGE_PARAMETER

val IrOverridableDeclaration<*>.isPromisifiedWrapperFakeOverride: Boolean
    get() = origin == PrepareSuspendFunctionsForExportLowering.PROMISIFIED_WRAPPER