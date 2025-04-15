/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationRemover
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.ICData
import org.jetbrains.kotlin.backend.common.serialization.KotlinFileSerializedData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.LoadedKlibs
import org.jetbrains.kotlin.ir.backend.js.generateModuleFragmentWithPlugins
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.util.PhaseType

fun generateIrForKlibSerialization(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    analysisResult: AnalysisResult,
    klibs: LoadedKlibs,
    icData: List<KotlinFileSerializedData>,
    irFactory: IrFactory,
    getDescriptorByLibrary: (KotlinLibrary) -> ModuleDescriptor,
): Pair<IrModuleFragment, IrPluginContext> {
    val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]
    performanceManager?.notifyPhaseStarted(PhaseType.TranslationToIr)

    val messageCollector = configuration.messageCollector
    val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), irFactory)
    val psi2Ir = Psi2IrTranslator(
        configuration.languageVersionSettings,
        Psi2IrConfiguration(ignoreErrors = false, configuration.partialLinkageConfig.isEnabled),
        messageCollector::checkNoUnboundSymbols
    )
    val psi2IrContext = psi2Ir.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext, symbolTable)

    val stubGenerator = DeclarationStubGeneratorImpl(
        psi2IrContext.moduleDescriptor,
        symbolTable,
        psi2IrContext.irBuiltIns,
        DescriptorByIdSignatureFinderImpl(psi2IrContext.moduleDescriptor, JsManglerDesc),
    )
    val irLinker = JsIrLinker(
        psi2IrContext.moduleDescriptor,
        messageCollector,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            builtIns = psi2IrContext.irBuiltIns,
            messageCollector = messageCollector
        ),
        ICData(icData.map { it.irData!! }, containsErrorCode = false),
        stubGenerator = stubGenerator
    )

    klibs.all.forEach { irLinker.deserializeOnlyHeaderModule(getDescriptorByLibrary(it), it) }

    val (moduleFragment, pluginContext) = psi2IrContext.generateModuleFragmentWithPlugins(
        project,
        files,
        irLinker,
        messageCollector,
        stubGenerator
    )

    if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
        irLinker.modules.forEach { fakeOverrideChecker.check(it) }
    }

    moduleFragment.accept(ExpectDeclarationRemover(psi2IrContext.symbolTable, false), null)

    return moduleFragment to pluginContext
}
