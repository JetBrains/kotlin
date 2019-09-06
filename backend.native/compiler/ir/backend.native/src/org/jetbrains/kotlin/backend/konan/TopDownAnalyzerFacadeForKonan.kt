/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isKonanStdlib
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.utils.KonanFactories
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.serialization.konan.KotlinResolvedModuleDescriptors
import org.jetbrains.kotlin.storage.StorageManager

internal object TopDownAnalyzerFacadeForKonan {

    fun analyzeFiles(files: Collection<KtFile>, context: Context): AnalysisResult {
        val config = context.config
        val moduleName = Name.special("<${config.moduleId}>") 

        val projectContext = ProjectContext(config.project, "TopDownAnalyzer for Konan")

        val module = KonanFactories.DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(
                moduleName, projectContext.storageManager, origin = CurrentKlibModuleOrigin)
        val moduleContext = MutableModuleContextImpl(module, projectContext)

        val resolvedDependencies = ResolvedDependencies(
                config.resolvedLibraries,
                projectContext.storageManager,
                module.builtIns,
                config.languageVersionSettings,
                config.friendModuleFiles)

        val additionalPackages = mutableListOf<PackageFragmentProvider>()
        if (!module.isKonanStdlib()) {
            val dependencies = listOf(module) + resolvedDependencies.moduleDescriptors.resolvedDescriptors + resolvedDependencies.moduleDescriptors.forwardDeclarationsModule
            module.setDependencies(dependencies, resolvedDependencies.friends)
        } else {
            assert (resolvedDependencies.moduleDescriptors.resolvedDescriptors.isEmpty())
            moduleContext.setDependencies(module)
            // [K][Suspend]FunctionN belong to stdlib.
            additionalPackages += functionInterfacePackageFragmentProvider(projectContext.storageManager, module)
        }

        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), moduleContext, context, additionalPackages)
    }

    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            context: Context,
            additionalPackages: List<PackageFragmentProvider> = emptyList()
    ): AnalysisResult {

        // we print out each file we compile if frontend phase is verbose
        files.takeIf {
            frontendPhase in context.phaseConfig.verbose
        } ?.forEach(::println)

        val analyzerForKonan = createTopDownAnalyzerProviderForKonan(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                context.config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!,
                additionalPackages
        ) {
            initContainer(context.config)
        }.apply {
            postprocessComponents(context, files)
        }.get<LazyTopDownAnalyzer>()

        analyzerForKonan.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
        return AnalysisResult.success(trace.bindingContext, moduleContext.module)
    }

    fun checkForErrors(files: Collection<KtFile>, bindingContext: BindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        for (file in files) {
            AnalyzingUtils.checkForSyntacticErrors(file)
        }
    }
}

private class ResolvedDependencies(
    resolvedLibraries: KotlinLibraryResolveResult,
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns,
    specifics: LanguageVersionSettings,
    friendModuleFiles: Set<File>
) {

    val moduleDescriptors: KotlinResolvedModuleDescriptors
    val friends: Set<ModuleDescriptorImpl>

    init {

        val collectedFriends = mutableListOf<ModuleDescriptorImpl>()

        val customAction: (KotlinLibrary, ModuleDescriptorImpl) -> Unit = { library, moduleDescriptor ->
            if (friendModuleFiles.contains(library.libraryFile)) {
                collectedFriends.add(moduleDescriptor)
            }
        }

        this.moduleDescriptors = KonanFactories.DefaultResolvedDescriptorsFactory.createResolved(
                resolvedLibraries, storageManager, builtIns, specifics, customAction)

        this.friends = collectedFriends.toSet()
    }
}

