/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal val mainMethodGenerationPhase = makeIrFilePhase(
    ::MainMethodGenerationLowering,
    name = "MainMethodGeneration",
    description = "Generate main bridges to parameterless mains, and wrappers for suspend mains.",
    prerequisite = setOf(jvmOverloadsAnnotationPhase)
)

private class MainMethodGenerationLowering(private val context: JvmBackendContext) : ClassLoweringPass {

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
                irClass.generateMainMethod { args ->
                    +irRunSuspend(mainMethod, args)
                }
            }
            return
        }

        irClass.functions.find { it.isParameterlessMainMethod() }?.let { parameterlessMainMethod ->
            irClass.generateMainMethod {
                if (parameterlessMainMethod.isSuspend) {
                    +irRunSuspend(parameterlessMainMethod, null)
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
        if (getJvmNameFromAnnotation() ?: name.asString() != "main") return false
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

    private fun IrClass.generateMainMethod(makeBody: IrBlockBodyBuilder.(IrValueParameter) -> Unit) =
        addFunction {
            name = Name.identifier("main")
            visibility = Visibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
            modality = Modality.FINAL
            this.origin = JvmLoweredDeclarationOrigin.GENERATED_EXTENDED_MAIN
        }.apply {
            val args = addValueParameter {
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
            body = context.createIrBuilder(symbol).irBlockBody { makeBody(args) }
        }

    private fun IrBuilderWithScope.irRunSuspend(target: IrSimpleFunction, args: IrValueParameter?): IrExpression {
        val backendContext = this@MainMethodGenerationLowering.context
        return irBlock {
            val wrapperConstructor = buildClass {
                name = Name.special("<main-wrapper>")
                visibility = JavaVisibilities.PACKAGE_VISIBILITY
                modality = Modality.FINAL
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            }.let { wrapper ->
                +wrapper

                wrapper.createImplicitParameterDeclarationWithWrappedDescriptor()

                val lambdaSuperClass = backendContext.ir.symbols.lambdaClass
                val functionClass = backendContext.ir.symbols.getJvmSuspendFunctionClass(0)

                wrapper.superTypes += lambdaSuperClass.defaultType
                wrapper.superTypes += functionClass.typeWith(backendContext.irBuiltIns.anyNType)
                wrapper.parent = target.parent

                val stringArrayType = backendContext.irBuiltIns.arrayClass.typeWith(backendContext.irBuiltIns.stringType)
                val argsField = args?.let { wrapper.addField("args", stringArrayType) }

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
                    visibility = JavaVisibilities.PACKAGE_VISIBILITY
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