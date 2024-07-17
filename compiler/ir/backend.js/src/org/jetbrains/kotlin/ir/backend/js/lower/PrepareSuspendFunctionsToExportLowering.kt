/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

internal class PrepareSuspendFunctionsToExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    companion object {
        private const val PROMISIFIED_WRAPPER_SUFFIX = "\$promisified"
        private const val ORIGINAL_FUNCTION_BODY_SAVER_SUFFIX = "\$original"

        val PROMISIFIED_WRAPPER by IrDeclarationOriginImpl
        val ORIGINAL_FUNCTION_BODY_SAVER by IrDeclarationOriginImpl
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

            if (declaration.isFakeOverride) return listOf(promisifiedWrapperFunction, declaration)

            return if (declaration.isTopLevel) {
                listOf(promisifiedWrapperFunction, declaration)
            } else {
                val originalBodySaverFunction = generateOriginalBodySaverFunction(declaration)

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
            valueParameters = originalFunc.valueParameters
            typeParameters = originalFunc.typeParameters
            dispatchReceiverParameter = originalFunc.dispatchReceiverParameter
            extensionReceiverParameter = originalFunc.extensionReceiverParameter
            contextReceiverParametersCount = originalFunc.contextReceiverParametersCount
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
            valueParameters = emptyList()
            typeParameters = emptyList()
        }

        originalFunc.copyParameterDeclarationsFrom(originalBodySaverFunction)
        originalBodySaverFunction.valueParameters.forEach { it.defaultValue = null }

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
                putValueArgument(
                    0,
                    JsIrBuilder.buildDynamicMemberExpression(irGet(dispatchReceiverParameter), originalName, dynamicType)
                )
                putValueArgument(
                    1,
                    JsIrBuilder.buildDynamicMemberExpression(
                        irCall(jsPrototypeFunctionSymbol).apply {
                            putValueArgument(0, irCall(jsClassFunctionSymbol).apply { putTypeArgument(0, dispatchReceiverParameter.type) })
                        },
                        originalName,
                        dynamicType
                    )
                )
            }
            +irReturn(
                irIfThenElse(
                    originalFunc.returnType,
                    isTheExportedFunctionOverridden,
                    irCallWithParametersOf(originalBodySaverFunction, originalFunc),
                    irCall(awaitFunctionSymbol).apply {
                        putValueArgument(
                            0,
                            irCallWithParametersOf(promisifiedWrapperFunction, originalFunc)
                        )
                    }
                )
            )
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
            copyParameterDeclarationsFrom(originalFunc)

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

            body = if (originalFunc.isFakeOverride) null else context.createIrBuilder(symbol).irBlockBody(this) {
                val call = irCallWithParametersOf(originalFunc, this@apply)

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
                        putValueArgument(
                            0,
                            JsIrBuilder.buildFunctionExpression(
                                IrSimpleTypeImpl(
                                    suspendFunctionClassSymbol,
                                    SimpleTypeNullability.NOT_SPECIFIED,
                                    listOf(call.type),
                                    emptyList()
                                ),
                                promisifiedSuspendLambda
                            )
                        )
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.irCallWithParametersOf(functionToCall: IrSimpleFunction, parametersSource: IrSimpleFunction): IrCall {
        return irCall(functionToCall.symbol).apply {
            dispatchReceiver = parametersSource.dispatchReceiverParameter?.let(::irGet)
            extensionReceiver = parametersSource.extensionReceiverParameter?.let(::irGet)
            parametersSource.valueParameters.forEachIndexed { index, irValueParameter ->
                putValueArgument(index, irGet(irValueParameter))
            }
        }
    }

    private fun IrMutableAnnotationContainer.addJsName(name: String) {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsNameAnnotation.symbol).apply {
            putValueArgument(0, name.toIrConst(context.irBuiltIns.stringType))
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
