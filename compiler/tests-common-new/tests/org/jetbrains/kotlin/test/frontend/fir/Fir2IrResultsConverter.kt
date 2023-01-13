/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class Fir2IrResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val isMppSupported = module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)

        // Create and initialize the module and its dependencies
        val project = compilerConfigurationProvider.getProject(module)
        // TODO: handle fir from light tree
        val ktFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.psi as KtFile? }
        val sourceFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.sourceFile }
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
            project, ktFiles, NoScopeRecordCliBindingTrace(), configuration,
            compilerConfigurationProvider.getPackagePartProviderFactory(module),
            ::FileBasedDeclarationProviderFactory, CompilerEnvironment,
            TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles), emptyList()
        )

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)

        val componentsMap = mutableMapOf<String, Fir2IrComponents>()
        val dependentIrParts = mutableListOf<JvmIrCodegenFactory.JvmIrBackendInput>()
        lateinit var mainIrPart: JvmIrCodegenFactory.JvmIrBackendInput

        var currentSymbolTable: SymbolTable? = null
        for ((index, firOutputPart) in inputArtifact.partsForDependsOnModules.withIndex()) {
            val dependentComponents = mutableListOf<Fir2IrComponents>()
            if (isMppSupported) {
                for (dependency in firOutputPart.module.dependsOnDependencies) {
                    dependentComponents.add(componentsMap[dependency.moduleName]!!)
                }
            }

            val (irModuleFragment, components, pluginContext) = firOutputPart.firAnalyzerFacade.convertToIr(
                fir2IrExtensions, dependentComponents, currentSymbolTable
            )
            currentSymbolTable = components.symbolTable
            componentsMap[firOutputPart.module.name] = components

            val irPart = JvmIrCodegenFactory.JvmIrBackendInput(
                irModuleFragment,
                components.symbolTable,
                phaseConfig,
                components.irProviders,
                fir2IrExtensions,
                FirJvmBackendExtension(firOutputPart.session, components),
                pluginContext,
                notifyCodegenStart = {},
            )

            if (index < inputArtifact.partsForDependsOnModules.size - 1) {
                dependentIrParts.add(irPart)
            } else {
                mainIrPart = irPart
            }
        }

        val mainModuleComponents = componentsMap[module.name]!!

        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        val generationState = GenerationState.Builder(
            project, ClassBuilderFactories.TEST,
            container.get(), NoScopeRecordCliBindingTrace().bindingContext, configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(mainModuleComponents)
        ).build()

        return IrBackendInput.JvmIrBackendInput(
            generationState,
            codegenFactory,
            dependentIrParts,
            mainIrPart,
            sourceFiles
        )
    }
}
