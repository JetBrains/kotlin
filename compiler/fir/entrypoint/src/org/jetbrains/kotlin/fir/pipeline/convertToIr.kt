/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable

data class FirResult(
    val platformOutput: ModuleCompilerAnalyzedOutput,
    val commonOutput: ModuleCompilerAnalyzedOutput?
)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

fun FirResult.convertToIrAndActualize(
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    linkViaSignatures: Boolean,
): Fir2IrResult {
    val result: Fir2IrResult

    if (commonOutput != null) {
        val commonIrOutput = commonOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            dependentComponents = emptyList(),
            currentSymbolTable = null
        )
        result = platformOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            dependentComponents = listOf(commonIrOutput.components),
            currentSymbolTable = commonIrOutput.components.symbolTable
        )
        IrActualizer.actualize(
            result.irModuleFragment,
            listOf(commonIrOutput.irModuleFragment)
        )
    } else {
        result = platformOutput.convertToIr(
            fir2IrExtensions,
            irGeneratorExtensions,
            linkViaSignatures = linkViaSignatures,
            dependentComponents = emptyList(),
            currentSymbolTable = null
        )
    }

    return result
}

private fun ModuleCompilerAnalyzedOutput.convertToIr(
    fir2IrExtensions: Fir2IrExtensions,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    linkViaSignatures: Boolean,
    dependentComponents: List<Fir2IrComponents>,
    currentSymbolTable: SymbolTable?
): Fir2IrResult {
    if (linkViaSignatures) {
        val signaturer = JvmIdSignatureDescriptor(mangler = JvmDescriptorMangler(mainDetector = null))
        return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            session, scopeSession, fir,
            session.languageVersionSettings, signaturer, fir2IrExtensions,
            FirJvmKotlinMangler(),
            JvmIrMangler, IrFactoryImpl, FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            irGeneratorExtensions,
            kotlinBuiltIns = DefaultBuiltIns.Instance, // TODO: consider passing externally
            generateSignatures = true,
            dependentComponents = dependentComponents,
            currentSymbolTable = currentSymbolTable
        )
    } else {
        return Fir2IrConverter.createModuleFragmentWithoutSignatures(
            session, scopeSession, fir,
            session.languageVersionSettings, fir2IrExtensions,
            FirJvmKotlinMangler(),
            JvmIrMangler, IrFactoryImpl, FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            irGeneratorExtensions,
            kotlinBuiltIns = DefaultBuiltIns.Instance, // TODO: consider passing externally,
            dependentComponents = dependentComponents,
            currentSymbolTable = currentSymbolTable
        )
    }
}
