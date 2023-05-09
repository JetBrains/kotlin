/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.KotlinMangler

data class FirResult(val outputs: List<ModuleCompilerAnalyzedOutput>)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

data class Fir2IrActualizedResult(
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizedResult: IrActualizedResult?,
)

fun FirResult.convertToIrAndActualizeForJvm(
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    diagnosticReporter: DiagnosticReporter,
): Fir2IrActualizedResult = this.convertToIrAndActualize(
    fir2IrExtensions,
    fir2IrConfiguration,
    irGeneratorExtensions,
    signatureComposer = signatureComposerForJvmFir2Ir(fir2IrConfiguration.linkViaSignatures),
    irMangler = JvmIrMangler,
    firMangler = FirJvmKotlinMangler(),
    visibilityConverter = FirJvmVisibilityConverter,
    diagnosticReporter = diagnosticReporter,
    kotlinBuiltIns = DefaultBuiltIns.Instance,
)

fun signatureComposerForJvmFir2Ir(generateSignatures: Boolean): IdSignatureComposer {
    val mangler = JvmDescriptorMangler(null)
    return if (generateSignatures) {
        JvmIdSignatureDescriptor(mangler)
    } else {
        DescriptorSignatureComposerStub(mangler)
    }
}

fun FirResult.convertToIrAndActualize(
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    signatureComposer: IdSignatureComposer,
    irMangler: KotlinMangler.IrMangler,
    firMangler: FirMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    diagnosticReporter: DiagnosticReporter,
    fir2IrResultPostCompute: Fir2IrResult.() -> Unit = {},
): Fir2IrActualizedResult {
    val fir2IrResult: Fir2IrResult
    val actualizationResult: IrActualizedResult?

    val commonMemberStorage = Fir2IrCommonMemberStorage(signatureComposer, firMangler)

    when (outputs.size) {
        0 -> error("No modules found")
        1 -> {
            fir2IrResult = outputs.single().convertToIr(
                fir2IrExtensions,
                fir2IrConfiguration,
                irGeneratorExtensions,
                commonMemberStorage = commonMemberStorage,
                irBuiltIns = null,
                irMangler,
                visibilityConverter,
                kotlinBuiltIns,
            ).also { result ->
                fir2IrResultPostCompute(result)
            }
            actualizationResult = null
        }
        else -> {
            val platformOutput = outputs.last()
            val commonOutputs = outputs.dropLast(1)
            var irBuiltIns: IrBuiltInsOverFir? = null
            val commonIrOutputs = commonOutputs.map {
                it.convertToIr(
                    fir2IrExtensions,
                    fir2IrConfiguration,
                    irGeneratorExtensions,
                    commonMemberStorage = commonMemberStorage,
                    irBuiltIns = irBuiltIns,
                    irMangler,
                    visibilityConverter,
                    kotlinBuiltIns,
                ).also { result ->
                    fir2IrResultPostCompute(result)
                    if (irBuiltIns == null) {
                        irBuiltIns = result.components.irBuiltIns
                    }
                }
            }
            fir2IrResult = platformOutput.convertToIr(
                fir2IrExtensions,
                fir2IrConfiguration,
                irGeneratorExtensions,
                commonMemberStorage = commonMemberStorage,
                irBuiltIns = irBuiltIns!!,
                irMangler,
                visibilityConverter,
                kotlinBuiltIns,
            ).also {
                fir2IrResultPostCompute(it)
            }

            actualizationResult = IrActualizer.actualize(
                fir2IrResult.irModuleFragment,
                commonIrOutputs.map { it.irModuleFragment },
                diagnosticReporter,
                fir2IrConfiguration.languageVersionSettings
            )
        }
    }

    val (irModuleFragment, components, pluginContext) = fir2IrResult
    return Fir2IrActualizedResult(irModuleFragment, components, pluginContext, actualizationResult)
}

private fun ModuleCompilerAnalyzedOutput.convertToIr(
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irBuiltIns: IrBuiltInsOverFir?,
    irMangler: KotlinMangler.IrMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
): Fir2IrResult {
    return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
        session, scopeSession, fir,
        fir2IrExtensions, fir2IrConfiguration,
        irMangler, IrFactoryImpl, visibilityConverter,
        Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation for other backends.
        irGeneratorExtensions,
        kotlinBuiltIns = kotlinBuiltIns,
        commonMemberStorage = commonMemberStorage,
        initializedIrBuiltIns = irBuiltIns
    )
}
