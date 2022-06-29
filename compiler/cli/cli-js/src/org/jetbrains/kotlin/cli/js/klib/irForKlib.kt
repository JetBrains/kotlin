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
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationRemover
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.ICData
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.backend.js.generateModuleFragmentWithPlugins
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

fun generateIrForKlibSerialization(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    analysisResult: AnalysisResult,
    sortedDependencies: Collection<KotlinLibrary>,
    icData: MutableList<KotlinFileSerializedData>,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    irFactory: IrFactory,
    verifySignatures: Boolean = true,
    getDescriptorByLibrary: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleFragment {
    val incrementalDataProvider = configuration.get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)
    val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
    val messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
    val allowUnboundSymbols = configuration[JSConfigurationKeys.PARTIAL_LINKAGE] ?: false

    val serializedIrFiles = mutableListOf<SerializedIrFile>()

    if (incrementalDataProvider != null) {
        val nonCompiledSources = files.associateBy { VfsUtilCore.virtualToIoFile(it.virtualFile) }
        val compiledIrFiles = incrementalDataProvider.serializedIrFiles
        val compiledMetaFiles = incrementalDataProvider.compiledPackageParts

        assert(compiledIrFiles.size == compiledMetaFiles.size)

        val storage = mutableListOf<KotlinFileSerializedData>()

        for (f in compiledIrFiles.keys) {
            if (f in nonCompiledSources) continue

            val irData = compiledIrFiles[f] ?: error("No Ir Data found for file $f")
            val metaFile = compiledMetaFiles[f] ?: error("No Meta Data found for file $f")
            val irFile = with(irData) {
                SerializedIrFile(
                    fileData,
                    String(fqn),
                    f.path.replace('\\', '/'),
                    types,
                    signatures,
                    strings,
                    bodies,
                    declarations,
                    debugInfo
                )
            }
            storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
        }

        icData.addAll(storage)
        serializedIrFiles.addAll(storage.map { it.irData })
    }

    val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), irFactory)
    val psi2Ir = Psi2IrTranslator(configuration.languageVersionSettings, Psi2IrConfiguration(errorPolicy.allowErrors, allowUnboundSymbols))
    val psi2IrContext = psi2Ir.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext, symbolTable)
    val irBuiltIns = psi2IrContext.irBuiltIns

    val feContext = psi2IrContext.run {
        JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
    }
    val irLinker = JsIrLinker(
        psi2IrContext.moduleDescriptor,
        messageLogger,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        feContext,
        ICData(serializedIrFiles, errorPolicy.allowErrors)
    )

    sortedDependencies.map { irLinker.deserializeOnlyHeaderModule(getDescriptorByLibrary(it), it) }

    val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, files, irLinker, messageLogger, expectDescriptorToSymbol)

    if (verifySignatures) {
        moduleFragment.acceptVoid(ManglerChecker(JsManglerIr, Ir2DescriptorManglerAdapter(JsManglerDesc)))
    }
    if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
        irLinker.modules.forEach { fakeOverrideChecker.check(it) }
    }

    if (configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) != true) {
        moduleFragment.transform(ExpectDeclarationRemover(psi2IrContext.symbolTable, false), null)
    }

    return moduleFragment
}

