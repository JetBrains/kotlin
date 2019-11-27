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
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal val mainMethodGenerationPhase = makeIrFilePhase(
    ::MainMethodGenerationLowering,
    name = "MainMethodGeneration",
    description = "Identify parameterless main methods and generate bridge main-methods",
    prerequisite = setOf(jvmOverloadsAnnotationPhase)
)

internal class MainMethodGenerationLowering(val context: JvmBackendContext) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (!context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)) return
        if (irClass.origin != IrDeclarationOrigin.FILE_CLASS) return

        val parameterlessMain = irClass.functions.find { it.isParameterlessMainMethod() } ?: return

        if (irClass.functions.any { it.isMainMethod() }) return

        generateMainMethod(irClass, parameterlessMain)
    }

    private fun generateMainMethod(irClass: IrClass, parameterlessMain: IrSimpleFunction) {
        irClass.addFunction {
            name = Name.identifier("main")
            visibility = Visibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
            modality = Modality.FINAL
            origin = JvmLoweredDeclarationOrigin.GENERATED_MAIN_FOR_PARAMETERLESS_MAIN_METHOD
        }.apply {
            addValueParameter {
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
            body = context.createIrBuilder(this.symbol).irBlockBody {
                +irReturn(this.irCall(parameterlessMain))
            }
        }
    }

    private fun IrSimpleFunction.isParameterlessMainMethod(): Boolean =
        typeParameters.isEmpty() && valueParameters.isEmpty() && returnType.isUnit() && name.asString() == "main"


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
}