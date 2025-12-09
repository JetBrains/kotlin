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
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering.Companion.bridgeFunction
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering.Companion.virtualBridgeFunction
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
import org.jetbrains.kotlin.ir.types.defaultType
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
 *       kotlin.coroutines.promisify { this.foo(a, b) }
 *
 *   @JsExport.Ignore
 *   // This function is supposed to be overridden from the Kotlin side
 *   suspend fun foo(a: Int, b: String): String { ... }
 *
 *   @JsExport.Ignore
 *   // This function is used on the call side
 *   suspend fun foo$suspendBridge(a: Int, b: String): String =
 *     if (this.foo === SomeClass.prototype.foo) {
 *       this.foo(a, b)
 *     } else {
 *       kotlin.coroutines.await(this.foo$promisified(a, b))
 *     }
 * }
 * ```
 *
 * Such a trick with the 3 functions helps us to call the original function instead of the promisified variant as long as the function
 * is not overridden from the JS side and also makes sure we don't recompile each child class if the body of the exported function
 * is changed.
 *
 * For interfaces, however, it requires one extra bridge, since we don't know on which side is the first implementor of an interface (on the TypeScript side, or on Kotlin),
 * so for such a case:
 * ```kotlin
 * @JsExport
 * interface SomeInterface {
 *   suspend fun foo(a: Int, b: String): String
 * }
 * ```
 *
 * We generate the following bridge functions:
 * ```kotlin
 * @JsExport
 * interface SomeInterface {
 *   suspend fun foo(a: Int, b: String): String
 *
 *   @JsName("foo")
 *   // This function to be implemented on the JS side
 *   fun foo$promisified(a: Int, b: String): Promise<String>
 *
 *   @JsExport.Ignore
 *   // This function is supposed to be implemented on the Kotlin side
 *   suspend fun foo(a: Int, b: String): String
 *
 *   @JsExport.Ignore
 *   // This function is used on the implementation call side, and should be added for each implementation on the Kotlin side
 *   suspend fun foo$suspendBridge(a: Int, b: String): String
 *
 *   @JsExport.Ignore
 *   // This function is used on the call side, if the dispatch receiver is an interface, and it has default implementation
 *   private suspend fun foo$virtualSuspendBridge(a: Int, b: String): String =
 *     if (jsTypeOf(this.foo$suspendBridge) === "function") {
 *          // If the implementor is a Kotlin class or its inheritor
 *          this.foo$suspendBridge(a, b)
 *     } else {
 *          // If the implementor is a JavaScript class or its inheritor
 *          kotlin.coroutines.await(this.foo$promisified(a, b))
 *     }
 * }
 * ```
 *
 */
internal class PrepareSuspendFunctionsForExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    companion object {
        private const val PROMISIFIED_WRAPPER_SUFFIX = "\$promisified"
        private const val EXPORTED_SUSPEND_FUNCTION_BRIDGE_SUFFIX = "\$suspendBridge"
        private const val EXPORTED_VIRTUAL_SUSPEND_FUNCTION_BRIDGE_SUFFIX = "\$virtualSuspendBridge"

        val PROMISIFIED_WRAPPER by IrDeclarationOriginImpl.Regular
        val EXPORTED_SUSPEND_FUNCTION_BRIDGE by IrDeclarationOriginImpl.Regular
        val EXPORTED_SUSPEND_FUNCTION_BRIDGE_PARAMETER by IrDeclarationOriginImpl.Regular

        var IrFunction.bridgeFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
        var IrFunction.virtualBridgeFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
        var IrFunction.promisifiedWrapperFunction: IrSimpleFunction? by irAttribute(copyByDefault = true)
    }

    private val dynamicType = context.dynamicType
    private val promiseClass = context.symbols.promiseClassSymbol
    private val jsNameAnnotation = context.symbols.jsNameAnnotationSymbol.owner.constructors.single()
    private val jsExportAnnotation = context.symbols.jsExportAnnotationSymbol.owner.constructors.single()
    private val awaitFunctionSymbol = context.symbols.awaitFunctionSymbol
    private val isMemberFunctionExists = context.symbols.isMemberFunctionExists
    private val jsClassFunctionSymbol = context.symbols.jsClass
    private val jsEqeqeqFunctionSymbol = context.symbols.jsEqeqeq
    private val promisifyFunctionSymbol = context.symbols.promisifyFunctionSymbol
    private val jsExportIgnoreAnnotation = context.symbols.jsExportIgnoreAnnotationSymbol.owner.constructors.single()
    private val jsPrototypeFunctionSymbol = context.symbols.jsPrototypeOfSymbol
    private val suspendFunctionClassSymbol = context.irBuiltIns.suspendFunctionN(0).symbol

    private val IrOverridableDeclaration<*>.isInterfaceMethod: Boolean
        get() = parentClassOrNull?.isInterface == true

    private val <T : IrOverridableDeclaration<*>> T.originallyExportedMember: T?
        get() = when {
            overriddenSymbols.isEmpty() -> runIf(isExported(context)) { this }
            else -> overriddenSymbols.firstNotNullOfOrNull {
                @Suppress("UNCHECKED_CAST")
                (it.owner as T).originallyExportedMember
            }
        }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        when {
            declaration !is IrSimpleFunction || !declaration.isSuspend -> null
            declaration.isTopLevel -> runIf(declaration.isExported(context)) {
                listOf(generatePromisifiedWrapper(declaration), declaration)
            }
            else -> {
                val originallyExportedSuspendMemberFunction = declaration.originallyExportedMember ?: return null
                val originalMemberIsInterfaceMethod = originallyExportedSuspendMemberFunction.isInterfaceMethod

                val promisifiedWrapperFunction = originallyExportedSuspendMemberFunction.promisifiedWrapperFunction
                    ?: generatePromisifiedWrapper(declaration, originalMemberIsInterfaceMethod)
                        .also { originallyExportedSuspendMemberFunction.promisifiedWrapperFunction = it }

                val implementationBridgeFunction = originallyExportedSuspendMemberFunction.bridgeFunction
                    ?: generateImplementorBridgeFunction(declaration, promisifiedWrapperFunction, originalMemberIsInterfaceMethod)
                        .also { originallyExportedSuspendMemberFunction.bridgeFunction = it }

                val virtualBridgeFunction = runIf(originalMemberIsInterfaceMethod) {
                    originallyExportedSuspendMemberFunction.virtualBridgeFunction
                        ?: generateVirtualBridgeFunction(declaration, promisifiedWrapperFunction, implementationBridgeFunction)
                            .also { originallyExportedSuspendMemberFunction.virtualBridgeFunction = it }
                }

                if (originallyExportedSuspendMemberFunction === declaration)
                    return listOfNotNull(declaration, promisifiedWrapperFunction, implementationBridgeFunction, virtualBridgeFunction)

                val currentDeclarationIsInterfaceMethod = declaration.isInterfaceMethod
                val currentDeclarationIsFirstClassMemberOverride = !currentDeclarationIsInterfaceMethod &&
                        declaration.overriddenSymbols.all { it.owner.isInterfaceMethod }

                declaration.bridgeFunction = implementationBridgeFunction
                declaration.promisifiedWrapperFunction = promisifiedWrapperFunction
                if (currentDeclarationIsInterfaceMethod) declaration.virtualBridgeFunction = virtualBridgeFunction

                return when {
                    // For the first class member overrides an exported interface suspend function, we should generate an implementation bridge
                    // containing the logic for this specific class. To minimize output, we could skip generating of such bridge functions for
                    // non-interface members and child classes of the first implementor class, since the bridge will be inherited through a prototype chain
                    currentDeclarationIsFirstClassMemberOverride -> listOf(
                        declaration,
                        generatePromisifiedWrapper(declaration),
                        generateImplementorBridgeFunction(declaration, promisifiedWrapperFunction, false),
                    )
                    // For real overrides of exported suspend member functions, we should generate a fake override of promisified wrapper
                    // since it could have different signature from the original suspend function. We need it only for TypeScript export,
                    // so this could be removed after migrating to Analysis API based TypeScript generation
                    declaration.isReal -> listOf(declaration, generatePromisifiedWrapper(declaration, isFakeOverride = true))
                    else -> null
                }
            }
        }

    private fun generateVirtualBridgeFunction(
        originalFunc: IrSimpleFunction,
        promisifiedWrapperFunction: IrSimpleFunction,
        implementationBridge: IrSimpleFunction,
    ): IrSimpleFunction =
        buildBridgeFunction("${originalFunc.name}$EXPORTED_VIRTUAL_SUSPEND_FUNCTION_BRIDGE_SUFFIX", originalFunc) { bridgeFunction ->
            bridgeFunction.visibility = DescriptorVisibilities.PRIVATE

            val dispatchReceiverParameter =
                bridgeFunction.dispatchReceiverParameter ?: compilationException(
                    "This function should be applied only to a member function",
                    bridgeFunction
                )

            val isFunctionImplementedOnKotlinSide = irCall(isMemberFunctionExists).apply {
                arguments[0] = irGet(dispatchReceiverParameter)
                arguments[1] = JsIrBuilder.buildRawReference(implementationBridge.symbol, dynamicType)
            }

            +irReturn(
                irIfThenElse(
                    bridgeFunction.returnType,
                    isFunctionImplementedOnKotlinSide,
                    irCallWithParametersOf(implementationBridge, bridgeFunction),
                    irCall(awaitFunctionSymbol).apply {
                        arguments[0] = irCallWithParametersOf(promisifiedWrapperFunction, bridgeFunction)
                    }
                )
            )
        }

    private fun generateImplementorBridgeFunction(
        originalFunc: IrSimpleFunction,
        promisifiedWrapperFunction: IrSimpleFunction,
        isInterfaceMethod: Boolean,
    ): IrSimpleFunction =
        buildBridgeFunction("${originalFunc.name}$EXPORTED_SUSPEND_FUNCTION_BRIDGE_SUFFIX", originalFunc) { bridgeFunction ->
            if (isInterfaceMethod) {
                bridgeFunction.modality = Modality.ABSTRACT
                return@buildBridgeFunction
            }

            val originalName =
                promisifiedWrapperFunction.getJsName() ?: compilationException(
                    "Promisified wrapper function should contain at least one @JsName annotation",
                    promisifiedWrapperFunction
                )

            val dispatchReceiverParameter =
                bridgeFunction.dispatchReceiverParameter ?: compilationException(
                    "This function should be applied only to a member function",
                    bridgeFunction
                )

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
                    bridgeFunction.returnType,
                    isExportedFunctionOverridden,
                    irCallWithParametersOf(originalFunc, bridgeFunction),
                    irCall(awaitFunctionSymbol).apply {
                        arguments[0] = irCallWithParametersOf(promisifiedWrapperFunction, bridgeFunction)
                    }
                )
            )
        }

    private inline fun buildBridgeFunction(
        name: String,
        originalFunc: IrSimpleFunction,
        bodyFactory: IrBlockBodyBuilder.(IrSimpleFunction) -> Unit,
    ): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(originalFunc)
            this.name = Name.identifier(name)
            origin = EXPORTED_SUSPEND_FUNCTION_BRIDGE
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
        }.also { bridgeFunc ->
            bridgeFunc.copyFunctionSignatureFrom(originalFunc)

            bridgeFunc.parent = originalFunc.parent

            bridgeFunc.annotations = buildList {
                add(JsIrBuilder.buildConstructorCall(jsExportIgnoreAnnotation.symbol))
                originalFunc.annotations.filterTo(this) { !it.isAnnotation(JsAnnotations.jsNameFqn) }
            }.compactIfPossible()

            bridgeFunc.parameters.forEach {
                it.defaultValue = null
                it.origin = EXPORTED_SUSPEND_FUNCTION_BRIDGE_PARAMETER
            }

            bridgeFunc.body = context.createIrBuilder(bridgeFunc.symbol)
                .irBlockBody(bridgeFunc) { bodyFactory(bridgeFunc) }
        }

    private fun generatePromisifiedWrapper(
        originalFunc: IrSimpleFunction,
        isInterfaceMethod: Boolean = false,
        isFakeOverride: Boolean = false,
    ): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(originalFunc)
            name = Name.identifier("${originalFunc.name.asString()}${PROMISIFIED_WRAPPER_SUFFIX}")
            origin = PROMISIFIED_WRAPPER
            isSuspend = false
            modality = Modality.OPEN
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            if (isFakeOverride) this.isFakeOverride = true
        }.apply {
            parent = originalFunc.parent
            copyFunctionSignatureFrom(originalFunc, returnType = promiseClass.typeWith(originalFunc.returnType))
            parameters.forEach {
                if (it.defaultValue != null) {
                    it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
                    it.defaultValue = null
                }
            }

            val (exportAnnotations, irrelevantAnnotations) = originalFunc.annotations.partition {
                it.isAnnotation(JsAnnotations.jsExportFqn) || it.isAnnotation(JsAnnotations.jsExportDefaultFqn)
            }

            annotations = exportAnnotations.compactIfPossible()

            addJsName(originalFunc.getJsNameOrKotlinName().identifier)

            if (originalFunc.isTopLevel) {
                if (!exportAnnotations.hasAnnotation(JsStandardClassIds.Annotations.JsExport)
                    && !exportAnnotations.hasAnnotation(JsStandardClassIds.Annotations.JsExportDefault)
                ) {
                    addJsExport()
                }
            }

            originalFunc.annotations = irrelevantAnnotations.compactIfPossible()

            if (isInterfaceMethod) {
                modality = Modality.ABSTRACT
            } else if (!isFakeOverride) {
                body = context.createIrBuilder(symbol).irBlockBody(this) {
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

    private fun IrBuilderWithScope.irCallWithParametersOf(functionToCall: IrSimpleFunction, parametersSource: IrSimpleFunction): IrCall =
        irCall(functionToCall.symbol).apply {
            parametersSource.parameters.forEachIndexed { index, irValueParameter ->
                arguments[index] = irGet(irValueParameter)
            }
            parametersSource.typeParameters.forEachIndexed { index, irTypeParameter ->
                typeArguments[index] = irTypeParameter.defaultType
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
                // In the case of super.suspendCall, as an optimization, we can keep the call without bridging it
                // since at compile time we know that it's always a Kotlin suspend function
                if (expression.superQualifierSymbol == null && expression.symbol.owner.isExportedSuspendFunction(context)) {
                    expression.symbol.owner
                        .let { it.virtualBridgeFunction ?: it.bridgeFunction }
                        ?.let { bridge -> expression.symbol = bridge.symbol }
                }

                return super.visitCall(expression)
            }
        })
    }
}

/** We need to ignore exporting of the original functions only after all the exported suspend function calls
 * were replaced with their bridge calls.
 *
 * If we do this to early (during the first lowering, as an example) the [isExportedSuspendFunction] will always return `false`
 **/
class IgnoreOriginalSuspendFunctionsThatWereExportedLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsExportIgnoreAnnotation = context.symbols.jsExportIgnoreAnnotationSymbol.owner.constructors.single()

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

val IrOverridableDeclaration<*>.isPromisifiedWrapper: Boolean
    get() = origin == PrepareSuspendFunctionsForExportLowering.PROMISIFIED_WRAPPER