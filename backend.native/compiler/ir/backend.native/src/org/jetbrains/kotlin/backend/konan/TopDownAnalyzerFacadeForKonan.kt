/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isKonanStdlib
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.konan.utils.KonanFactories
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDescriptorFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptors
import org.jetbrains.kotlin.storage.StorageManager

object TopDownAnalyzerFacadeForKonan {

    fun analyzeFiles(files: Collection<KtFile>, config: KonanConfig): AnalysisResult {
        val moduleName = Name.special("<${config.moduleId}>") 

        val projectContext = ProjectContext(config.project)

        val module = DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(
                moduleName, projectContext.storageManager, origin = CurrentKonanModuleOrigin)
        val context = MutableModuleContextImpl(module, projectContext)

        val resolvedDependencies = ResolvedDependencies(
                config.resolvedLibraries,
                projectContext.storageManager,
                module.builtIns,
                config.languageVersionSettings,
                config.friendModuleFiles)

        if (!module.isKonanStdlib()) {
            val dependencies = listOf(module) + resolvedDependencies.moduleDescriptors.resolvedDescriptors + resolvedDependencies.moduleDescriptors.forwardDeclarationsModule
            module.setDependencies(dependencies, resolvedDependencies.friends)
        } else {
            assert (resolvedDependencies.moduleDescriptors.resolvedDescriptors.isEmpty())
            context.setDependencies(module)
        }

        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), context, config)
    }

    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            config: KonanConfig
    ): AnalysisResult {

        // we print out each file we compile if frontend phase is verbose
        files.takeIf { with (KonanPhases) {
            phases[known(KonanPhase.FRONTEND.visibleName)]!!.verbose
        }} ?.forEach(::println)

        val analyzerForKonan = createTopDownAnalyzerForKonan(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!
        )

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
        resolvedLibraries: KonanLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        specifics: LanguageVersionSettings,
        friendModuleFiles: Set<File>
) {

    val moduleDescriptors: KonanResolvedModuleDescriptors
    val friends: Set<ModuleDescriptorImpl>

    init {

        val collectedFriends = mutableListOf<ModuleDescriptorImpl>()

        val customAction: (KonanLibrary, ModuleDescriptorImpl) -> Unit = { library, moduleDescriptor ->
            if (friendModuleFiles.contains(library.libraryFile)) {
                collectedFriends.add(moduleDescriptor)
            }
        }

        this.moduleDescriptors = KonanFactories.DefaultResolvedDescriptorsFactory.createResolved(
                resolvedLibraries, storageManager, builtIns, specifics, customAction)

        this.friends = collectedFriends.toSet()
    }
}

