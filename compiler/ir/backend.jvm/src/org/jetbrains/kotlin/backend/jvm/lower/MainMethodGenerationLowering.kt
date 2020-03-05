/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.allParameters
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
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFileClass
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

    private fun IrBuilderWithScope.irRunSuspend(target: IrSimpleFunction, args: IrValueParameter?) =
        irCall(this@MainMethodGenerationLowering.context.ir.symbols.runSuspendFunction).apply {
            val reference = IrFunctionReferenceImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                this@MainMethodGenerationLowering.context.ir.symbols.suspendFunctionN(0).typeWith(context.irBuiltIns.anyNType),
                target.symbol,
                typeArgumentsCount = 0,
                reflectionTarget = null
            )
            if (args != null) {
                reference.putValueArgument(0, irGet(args))
            }
            putValueArgument(0, reference)
        }
}