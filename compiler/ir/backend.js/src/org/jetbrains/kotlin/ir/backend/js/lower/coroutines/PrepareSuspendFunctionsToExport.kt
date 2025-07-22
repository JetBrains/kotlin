/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

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
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.compileLambdasAsEs6ArrowFunctions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
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
 *  @JsName("foo")
 *  // This function is used from the JS side and is supposed to be overridden from the JS side
 *  fun foo$promisified(a: Int, b: String): Promise<String> =
 *      kotlin.coroutines.promisify { this.foo$original(a, b) }
 *
 *   @JsExport.Ignore
 *   // This function is supposed to be overridden from the Kotlin side
 *   suspend fun foo$original(a: Int, b: String): String { ... }
 *
 *   @JsExport.Ignore
 *   // This function is used on the call side
 *   open suspend fun foo(a: Int, b: String): String =
 *     if (this.foo === SomeClass.prototype.foo) {
 *       this.foo$original(a, b)
 *     } else {
 *       kotlin.coroutines.await(this.foo$promisified(a, b))
 *     }
 * }
 * ```
 *
 * Such a trick with the 3 functions helps us to call the original function instead of the promisified variant until the function is not overridden from the JS side
 * and also helps us to not recompile each child class if the body of the exported function is changed
 */
internal class PrepareSuspendFunctionsToExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    companion object {
        private const val PROMISIFIED_WRAPPER_SUFFIX = "\$promisified"
        private const val ORIGINAL_FUNCTION_BODY_SAVER_SUFFIX = "\$original"

        val PROMISIFIED_WRAPPER by IrDeclarationOriginImpl
        val ORIGINAL_FUNCTION_BODY_SAVER by IrDeclarationOriginImpl
        val PROXIED_SUSPEND_FUNCTION_PARAMETER by IrDeclarationOriginImpl

        var IrFunction.wrapperFunction: IrFunction? by irAttribute(copyByDefault = true)
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

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isExportedSuspendFunction(context)) {
            val realOverrideTarget = declaration.realOverrideTarget
            val promisifiedWrapperFunction = generatePromisifiedWrapper(declaration)

            if (declaration.isFakeOverride) return listOf(
                promisifiedWrapperFunction.apply { body = generatePromisifiedWrapperBody(declaration, this) },
                declaration
            )

            return if (declaration.isTopLevel) {
                listOf(
                    promisifiedWrapperFunction.apply { body = generatePromisifiedWrapperBody(declaration, this) },
                    declaration
                )
            } else {
                val originalBodySaverFunction = generateOriginalBodySaverFunction(declaration).apply {
                    wrapperFunction = promisifiedWrapperFunction
                }

                promisifiedWrapperFunction.body = generatePromisifiedWrapperBody(originalBodySaverFunction, promisifiedWrapperFunction)

                if (realOverrideTarget === declaration) {
                    declaration.replaceBodyWithBridge(originalBodySaverFunction, promisifiedWrapperFunction)
                }

                return listOf(promisifiedWrapperFunction, originalBodySaverFunction, declaration)
            }
        }

        return null
    }

    private fun generateOriginalBodySaverFunction(originalFunc: IrSimpleFunction): IrSimpleFunction {
        return context.irFactory.buildFun {
            updateFrom(originalFunc)
            name = Name.identifier("${originalFunc.name}${ORIGINAL_FUNCTION_BODY_SAVER_SUFFIX}")
            origin = ORIGINAL_FUNCTION_BODY_SAVER
            returnType = originalFunc.returnType
        }.apply {
            parent = originalFunc.parent
            parameters = originalFunc.parameters
            typeParameters = originalFunc.typeParameters
            body = originalFunc.body
            annotations = originalFunc.annotations memoryOptimizedPlus listOf(
                JsIrBuilder.buildConstructorCall(jsExportIgnoreAnnotation.symbol)
            )
        }
    }

    private fun IrSimpleFunction.replaceBodyWithBridge(
        originalBodySaverFunction: IrSimpleFunction,
        promisifiedWrapperFunction: IrSimpleFunction,
    ) {
        val originalFunc = this.apply {
            parameters = emptyList()
            typeParameters = emptyList()
        }

        originalFunc.copyParametersFrom(originalBodySaverFunction)
        originalFunc.copyTypeParametersFrom(originalBodySaverFunction)
        originalFunc.parameters.forEach {
            it.defaultValue = null
            it.origin = PROXIED_SUSPEND_FUNCTION_PARAMETER
        }

        val originalName =
            promisifiedWrapperFunction.getJsName() ?: compilationException(
                "Promisified wrapper function should contain at least one @JsName annotation",
                promisifiedWrapperFunction
            )

        val dispatchReceiverParameter =
            originalFunc.dispatchReceiverParameter ?: compilationException(
                "This function should be applied only to a member function",
                originalFunc
            )

        originalFunc.body = context.createIrBuilder(originalFunc.symbol).irBlockBody(originalFunc) {
            val isTheExportedFunctionOverridden = irCall(jsEqeqeqFunctionSymbol).apply {
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
                    originalFunc.returnType,
                    isTheExportedFunctionOverridden,
                    irCallWithParametersOf(originalBodySaverFunction, originalFunc),
                    irCall(awaitFunctionSymbol).apply {
                        arguments[0] = irCallWithParametersOf(promisifiedWrapperFunction, originalFunc)
                    }
                )
            )
        }
    }

    private fun generatePromisifiedWrapperBody(
        originalFunc: IrSimpleFunction,
        promisifiedWrapperFunction: IrSimpleFunction,
    ): IrBlockBody? {
        return if (originalFunc.isFakeOverride) null else context.createIrBuilder(promisifiedWrapperFunction.symbol)
            .irBlockBody(promisifiedWrapperFunction) {
                val call = irCallWithParametersOf(originalFunc, promisifiedWrapperFunction)

                if (!this@PrepareSuspendFunctionsToExportLowering.context.configuration.compileLambdasAsEs6ArrowFunctions) {
                    val selfSaver = call.dispatchReceiver?.let(::createTmpVariable)
                    call.dispatchReceiver = selfSaver?.let(::irGet)
                }

                val promisifiedSuspendLambda = context.irFactory.buildFun {
                    name = SpecialNames.NO_NAME_PROVIDED
                    visibility = DescriptorVisibilities.LOCAL
                    isSuspend = true
                    returnType = call.type
                }.also {
                    it.parent = promisifiedWrapperFunction
                    it.body = irBlockBody(it) { +irReturn(call) }
                }

                +irReturn(
                    irCall(promisifyFunctionSymbol).apply {
                        arguments[0] = JsIrBuilder.buildFunctionExpression(
                            IrSimpleTypeImpl(
                                suspendFunctionClassSymbol, SimpleTypeNullability.NOT_SPECIFIED, listOf(call.type), emptyList()
                            ), promisifiedSuspendLambda
                        )
                    })
            }
    }

    private fun generatePromisifiedWrapper(originalFunc: IrSimpleFunction): IrSimpleFunction {
        return context.irFactory.buildFun {
            updateFrom(originalFunc)
            name = Name.identifier("${originalFunc.name.asString()}${PROMISIFIED_WRAPPER_SUFFIX}")
            origin = PROMISIFIED_WRAPPER
            isSuspend = false
            isFakeOverride = originalFunc.isFakeOverride
            modality = Modality.FINAL
            returnType =
                IrSimpleTypeImpl(promiseClass, SimpleTypeNullability.NOT_SPECIFIED, listOf(originalFunc.returnType), emptyList())
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
        }.apply {
            parent = originalFunc.parent
            copyTypeParametersFrom(originalFunc)
            copyParametersFrom(originalFunc)
            parameters.forEach {
                if (it.defaultValue != null) {
                    it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
                    it.defaultValue = null
                }
            }

            val (exportAnnotations, irrelevantAnnotations) = originalFunc.annotations.partition {
                it.isAnnotation(JsAnnotations.jsExportFqn) || it.isAnnotation(JsAnnotations.jsNameFqn)
            }

            annotations = annotations memoryOptimizedPlus exportAnnotations

            if (!exportAnnotations.any { it.isAnnotation(JsAnnotations.jsNameFqn) }) {
                addJsName(originalFunc.name.asString())
            }
            if (originalFunc.isTopLevel && !exportAnnotations.any { it.isAnnotation(JsAnnotations.jsExportFqn) }) {
                addJsExport()
            }

            originalFunc.annotations = irrelevantAnnotations
        }
    }

    private fun IrBuilderWithScope.irCallWithParametersOf(functionToCall: IrSimpleFunction, parametersSource: IrSimpleFunction): IrCall {
        return irCall(functionToCall.symbol).apply {
            parametersSource.parameters.forEachIndexed { index, irValueParameter ->
                arguments[index] = irGet(irValueParameter)
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

class IgnoreOriginalSuspendFunctionsThatWereExportedLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsExportIgnoreAnnotation = context.intrinsics.jsExportIgnoreAnnotationSymbol.owner.constructors.single()

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isExportedSuspendFunction(context)) {
            declaration.annotations =
                declaration.annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportIgnoreAnnotation.symbol)
        }

        return null
    }
}

private fun IrSimpleFunction.isExportedSuspendFunction(context: JsIrBackendContext): Boolean =
    isSuspend && isExported(context)

val IrValueParameter.isProxyParameterForExportedSuspendFunction: Boolean
    get() = origin == PrepareSuspendFunctionsToExportLowering.PROXIED_SUSPEND_FUNCTION_PARAMETER
