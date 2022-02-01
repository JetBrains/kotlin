/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal class MainMethodGenerationLowering(private val context: JvmBackendContext) : ClassLoweringPass {

    /**
     * This pass finds extended main methods and introduces a regular
     * `public static void main(String[] args)` entry point, as appropriate:
     *   - invocation via [kotlin.coroutines.jvm.internal.runSuspend] suspend main methods.
     *   - a simple delegating wrapper for parameterless main methods
     *
     * There are three cases that must be handled, in order of precedence:
     *
     * 1. `suspend fun main(args: Array<String>) { .. }` for which we generate
     *    ```
     *    fun main(args: Array<String>) {
     *      runSuspend { main(args) }
     *    }
     *    ```
     *
     * 2. `suspend fun main() { .. }` for which we generate
     *    ```
     *    fun main(args: Array<String>) {
     *      runSuspend { main() }
     *    }
     *    ```
     *
     * 3. `fun main() { .. }` for which we generate
     *    ```
     *    fun main(args: Array<String>) {
     *      main()
     *    }
     *    ```
     */
    override fun lower(irClass: IrClass) {
        if (!context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)) return
        if (!irClass.isFileClass) return

        irClass.functions.find { it.isMainMethod() }?.let { mainMethod ->
            if (mainMethod.isSuspend) {
                irClass.generateMainMethod { newMain, args ->
                    +irRunSuspend(mainMethod, args, newMain)
                }
            }
            return
        }

        irClass.functions.find { it.isParameterlessMainMethod() }?.let { parameterlessMainMethod ->
            irClass.generateMainMethod { newMain, _ ->
                if (parameterlessMainMethod.isSuspend) {
                    +irRunSuspend(parameterlessMainMethod, null, newMain)
                } else {
                    +irCall(parameterlessMainMethod)
                }
            }
        }
    }

    private fun IrSimpleFunction.isParameterlessMainMethod(): Boolean =
        typeParameters.isEmpty() &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty() &&
                returnType.isUnit() &&
                name.asString() == "main"

    private fun IrSimpleFunction.isMainMethod(): Boolean {
        if ((getJvmNameFromAnnotation() ?: name.asString()) != "main") return false
        if (!returnType.isUnit()) return false

        val parameter = allParameters.singleOrNull() ?: return false
        if (!parameter.type.isArray() && !parameter.type.isNullableArray()) return false

        val argType = (parameter.type as IrSimpleType).arguments.first()
        return when (argType) {
            is IrTypeProjection -> {
                (argType.variance != Variance.IN_VARIANCE) && argType.type.isStringClassType()
            }
            else -> false
        }
    }

    private fun IrClass.generateMainMethod(makeBody: IrBlockBodyBuilder.(IrSimpleFunction, IrValueParameter) -> Unit) =
        addFunction {
            name = Name.identifier("main")
            visibility = DescriptorVisibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.GENERATED_EXTENDED_MAIN
        }.apply {
            val args = addValueParameter {
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
            body = context.createIrBuilder(symbol).irBlockBody { makeBody(this@apply, args) }
        }

    private fun IrBuilderWithScope.irRunSuspend(
        target: IrSimpleFunction,
        args: IrValueParameter?,
        newMain: IrSimpleFunction
    ): IrExpression {
        val backendContext = this@MainMethodGenerationLowering.context
        return irBlock {
            val wrapperConstructor = backendContext.irFactory.buildClass {
                name = Name.special("<main-wrapper>")
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                modality = Modality.FINAL
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            }.let { wrapper ->
                +wrapper

                wrapper.createImplicitParameterDeclarationWithWrappedDescriptor()

                val lambdaSuperClass = backendContext.ir.symbols.lambdaClass
                val functionClass = backendContext.ir.symbols.getJvmSuspendFunctionClass(0)

                wrapper.superTypes += lambdaSuperClass.defaultType
                wrapper.superTypes += functionClass.typeWith(backendContext.irBuiltIns.anyNType)
                wrapper.parent = newMain

                val stringArrayType = backendContext.irBuiltIns.arrayClass.typeWith(backendContext.irBuiltIns.stringType)
                val argsField = args?.let {
                    wrapper.addField {
                        name = Name.identifier("args")
                        type = stringArrayType
                        visibility = DescriptorVisibilities.PRIVATE
                        origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                    }
                }

                wrapper.addFunction("invoke", backendContext.irBuiltIns.anyNType, isSuspend = true).also { invoke ->
                    val invokeToOverride = functionClass.functions.single()

                    invoke.overriddenSymbols += invokeToOverride
                    invoke.body = backendContext.createIrBuilder(invoke.symbol).irBlockBody {
                        +irReturn(irCall(target.symbol).also { call ->
                            if (args != null) {
                                call.putValueArgument(0, irGetField(irGet(invoke.dispatchReceiverParameter!!), argsField!!))
                            }
                        })
                    }
                }

                wrapper.addConstructor {
                    isPrimary = true
                    visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                }.also { constructor ->
                    val superClassConstructor = lambdaSuperClass.owner.constructors.single()
                    val param = args?.let { constructor.addValueParameter("args", stringArrayType) }

                    constructor.body = backendContext.createIrBuilder(constructor.symbol).irBlockBody {
                        +irDelegatingConstructorCall(superClassConstructor).also {
                            it.putValueArgument(0, irInt(1))
                        }
                        if (args != null) {
                            +irSetField(irGet(wrapper.thisReceiver!!), argsField!!, irGet(param!!))
                        }
                    }
                }
            }

            +irCall(backendContext.ir.symbols.runSuspendFunction).apply {
                putValueArgument(
                    0, IrConstructorCallImpl.fromSymbolOwner(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        wrapperConstructor.returnType,
                        wrapperConstructor.symbol
                    ).also {
                        if (args != null) {
                            it.putValueArgument(0, irGet(args))
                        }
                    }
                )
            }
        }
    }
}
