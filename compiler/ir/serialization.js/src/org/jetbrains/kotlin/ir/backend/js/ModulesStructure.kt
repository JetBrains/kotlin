/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.toLogger
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForWeb
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import java.io.File
import kotlin.collections.set

sealed class MainModule {
    class SourceFiles(val files: List<KtFile>) : MainModule()
    class Klib(val libPath: String) : MainModule()
}

class ModulesStructure(
    val project: Project,
    val mainModule: MainModule,
    val compilerConfiguration: CompilerConfiguration,
    private val libraryPaths: Collection<String>,
    friendDependenciesPaths: Collection<String>,
) {

    private val allDependenciesResolution = CommonKLibResolver.resolveWithoutDependencies(
        libraryPaths,
        compilerConfiguration.messageCollector.toLogger(),
        compilerConfiguration.get(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR),
        duplicatedUniqueNameStrategy = compilerConfiguration.get(
            KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
            DuplicatedUniqueNameStrategy.DENY
        ),
    )

    val allDependencies: List<KotlinLibrary>
        get() = allDependenciesResolution.libraries

    val friendDependencies: List<KotlinLibrary> = allDependencies.run {
        val friendAbsolutePaths = friendDependenciesPaths.map { File(it).canonicalPath }
        memoryOptimizedFilter {
            it.libraryFile.absolutePath in friendAbsolutePaths
        }
    }

    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> by lazy {
        val transitives = allDependenciesResolution.resolveWithDependencies().getFullResolvedList()
        transitives.associate { klib ->
            klib.library to klib.resolvedDependencies.map { d -> d.library }
        }.toMap()
    }

    private val builtInsDep: KotlinLibrary? = allDependencies.find { it.isBuiltIns }

    class JsFrontEndResult(val jsAnalysisResult: AnalysisResult, val hasErrors: Boolean) {
        val moduleDescriptor: ModuleDescriptor
            get() = jsAnalysisResult.moduleDescriptor

        val bindingContext: BindingContext
            get() = jsAnalysisResult.bindingContext
    }

    lateinit var jsFrontEndResult: JsFrontEndResult

    fun runAnalysis(
        analyzer: AbstractAnalyzerWithCompilerReport,
        analyzerFacade: AbstractTopDownAnalyzerFacadeForWeb
    ) {
        require(mainModule is MainModule.SourceFiles)
        val files = mainModule.files

        analyzer.analyzeAndReport(files) {
            analyzerFacade.analyzeFiles(
                files = files,
                project = project,
                configuration = compilerConfiguration,
                moduleDescriptors = descriptors.values.toList(),
                friendModuleDescriptors = friendDependencies.map { getModuleDescriptor(it) },
                targetEnvironment = analyzer.targetEnvironment,
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val analysisResult = analyzer.analysisResult
        if (compilerConfiguration.getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)) {
            val shouldGoToNextIcRound = shouldGoToNextIcRound(compilerConfiguration) {
                KlibMetadataIncrementalSerializer(
                    files,
                    compilerConfiguration,
                    project,
                    analysisResult.bindingContext,
                    analysisResult.moduleDescriptor,
                )
            }
            if (shouldGoToNextIcRound) {
                throw IncrementalNextRoundException()
            }
        }

        if (analyzer.hasErrors() || analysisResult !is JsAnalysisResult) {
            throw CompilationErrorException()
        }

        val hasErrors = analyzerFacade.checkForErrors(files, analysisResult.bindingContext)

        jsFrontEndResult = JsFrontEndResult(analysisResult, hasErrors)
    }

    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    private val _descriptors: MutableMap<KotlinLibrary, ModuleDescriptorImpl> = mutableMapOf()

    init {
        val descriptors = allDependencies.map { getModuleDescriptorImpl(it) }
        val friendDescriptors = friendDependencies.mapTo(mutableSetOf(), ::getModuleDescriptorImpl)
        descriptors.forEach { descriptor ->
            descriptor.setDependencies(descriptors, friendDescriptors)
        }
    }

    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
    val descriptors: Map<KotlinLibrary, ModuleDescriptor>
        get() = _descriptors

    private fun getModuleDescriptorImpl(current: KotlinLibrary): ModuleDescriptorImpl {
        if (current in _descriptors) {
            return _descriptors.getValue(current)
        }

        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val lookupTracker = compilerConfiguration[CommonConfigurationKeys.LOOKUP_TRACKER] ?: LookupTracker.DO_NOTHING
        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            languageVersionSettings,
            storageManager,
            runtimeModule?.builtIns,
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
        )
        if (isBuiltIns) runtimeModule = md

        _descriptors[current] = md

        return md
    }

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptor =
        getModuleDescriptorImpl(current)

    val builtInModuleDescriptor: ModuleDescriptor? =
        if (builtInsDep != null)
            getModuleDescriptor(builtInsDep)
        else
            null // null in case compiling builtInModule itself
}

// Considering library built-ins if it has no dependencies.
// All non-built-ins libraries must have built-ins as a dependency.
private val KotlinLibrary.isBuiltIns: Boolean
    get() = manifestProperties
        .propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
        .isEmpty()
