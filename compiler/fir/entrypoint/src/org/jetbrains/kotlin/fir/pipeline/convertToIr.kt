/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.KotlinMangler

data class FirResult(
    val platformOutput: ModuleCompilerAnalyzedOutput,
    val commonOutput: ModuleCompilerAnalyzedOutput?
)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

fun FirResult.convertToIrAndActualizeForJvm(
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    linkViaSignatures: Boolean,
): Fir2IrResult = this.convertToIrAndActualize(
    fir2IrExtensions,
    irGeneratorExtensions,
    linkViaSignatures = linkViaSignatures,
    signatureComposerCreator = { JvmIdSignatureDescriptor(JvmDescriptorMangler(null)) },
    irMangler = JvmIrMangler,
    visibilityConverter = FirJvmVisibilityConverter,
    kotlinBuiltIns = DefaultBuiltIns.Instance,
)

fun FirResult.convertToIrAndActualize(
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    linkViaSignatures: Boolean,
    signatureComposerCreator: (() -> IdSignatureComposer)?,
    irMangler: KotlinMangler.IrMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    fir2IrResultPostCompute: Fir2IrResult.() -> Unit = {},
): Fir2IrResult {
    val result: Fir2IrResult

    val commonMemberStorage = Fir2IrCommonMemberStorage(
        generateSignatures = linkViaSignatures,
        signatureComposerCreator = signatureComposerCreator,
        manglerCreator = { FirJvmKotlinMangler() } // TODO: replace with potentially simpler version for other backends.
    )

    if (commonOutput != null) {
        val commonIrOutput = commonOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            commonMemberStorage = commonMemberStorage,
            irBuiltIns = null,
            irMangler,
            visibilityConverter,
            kotlinBuiltIns,
        ).also {
            fir2IrResultPostCompute(it)
        }
        result = platformOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            commonMemberStorage = commonMemberStorage,
            irBuiltIns = commonIrOutput.components.irBuiltIns,
            irMangler,
            visibilityConverter,
            kotlinBuiltIns,
        ).also {
            fir2IrResultPostCompute(it)
        }
        IrActualizer.actualize(
            result.irModuleFragment,
            listOf(commonIrOutput.irModuleFragment)
        )
    } else {
        result = platformOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            commonMemberStorage = commonMemberStorage,
            irBuiltIns = null,
            irMangler,
            visibilityConverter,
            kotlinBuiltIns,
        )
    }

    return result
}

private fun ModuleCompilerAnalyzedOutput.convertToIr(
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    linkViaSignatures: Boolean,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    irBuiltIns: IrBuiltInsOverFir?,
    irMangler: KotlinMangler.IrMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
): Fir2IrResult {
    return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
        session, scopeSession, fir,
        session.languageVersionSettings, fir2IrExtensions,
        irMangler, IrFactoryImpl, visibilityConverter,
        Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation for other backends.
        irGeneratorExtensions,
        kotlinBuiltIns = kotlinBuiltIns,
        generateSignatures = linkViaSignatures,
        commonMemberStorage = commonMemberStorage,
        initializedIrBuiltIns = irBuiltIns
    )
}
