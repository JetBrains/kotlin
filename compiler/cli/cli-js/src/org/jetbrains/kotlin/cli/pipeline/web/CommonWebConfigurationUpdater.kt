/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.allowKotlinPackage
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.js.*
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

/**
 * Contains configuration updating logic shared between JS and WASM CLIs
 */
object CommonWebConfigurationUpdater : ConfigurationUpdater<K2JSCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val (arguments, services, rootDisposable, _, _) = input
        setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
        initializeCommonConfiguration(configuration, arguments)
        checkOutputArguments(arguments, configuration.messageCollector)

        configuration.wasmCompilation = arguments.wasm
        arguments.includes?.let { configuration.includes = it }
        configuration.produceKlibFile = arguments.irProduceKlibFile
        configuration.produceKlibDir = arguments.irProduceKlibDir
        configuration.granularity = arguments.granularity
        configuration.tsCompilationStrategy = arguments.dtsStrategy
        arguments.main?.let { configuration.callMainMode = it }

        val zipAccessor = DisposableZipFileSystemAccessor(64)
        Disposer.register(rootDisposable, zipAccessor)
        configuration.zipFileSystemAccessor = zipAccessor

//        TODO: move to facade
//        if (arguments.verbose) {
//            reportCompiledSourcesList(messageCollector, sourcesFiles)
//        }

        configuration.perModuleOutputName = arguments.irPerModuleOutputName
    }

    /**
     * This part of the configuration update is shared between phased K2 CLI and
     * K1 implementation of [K2JSCompiler.setupPlatformSpecificArgumentsAndServices].
     */
    internal fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services,
    ) {
        val messageCollector = configuration.messageCollector
        @Suppress("DEPRECATION")
        if (arguments.outputFile != null) {
            messageCollector.report(WARNING, "The '-output' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.noStdlib) {
            messageCollector.report(WARNING, "The '-no-stdlib' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.metaInfo) {
            messageCollector.report(WARNING, "The '-meta-info' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.typedArrays) {
            messageCollector.report(
                WARNING,
                "The '-Xtyped-arrays' command line option does nothing and will be removed in a future release"
            )
        }

        if (arguments.debuggerCustomFormatters) {
            configuration.useDebuggerCustomFormatters = true
        }

        if (arguments.sourceMap) {
            configuration.sourceMap = true
            if (arguments.sourceMapPrefix != null) {
                configuration.sourceMapPrefix = arguments.sourceMapPrefix!!
            }

            var sourceMapSourceRoots = arguments.sourceMapBaseDirs
            if (sourceMapSourceRoots == null && StringUtil.isNotEmpty(arguments.sourceMapPrefix)) {
                sourceMapSourceRoots = calculateSourceMapSourceRoot(messageCollector, arguments)
            }

            if (sourceMapSourceRoots != null) {
                val sourceMapSourceRootList = StringUtil.split(sourceMapSourceRoots, File.pathSeparator)
                configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceMapSourceRootList)
            }

        } else {
            if (arguments.sourceMapPrefix != null) {
                messageCollector.report(WARNING, "source-map-prefix argument has no effect without source map", null)
            }
            if (arguments.sourceMapBaseDirs != null) {
                messageCollector.report(WARNING, "source-map-source-root argument has no effect without source map", null)
            }
        }

        configuration.friendPathsDisabled = arguments.friendModulesDisabled
        configuration.generateStrictImplicitExport = arguments.strictImplicitExportType

        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            configuration.addAll(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        if (arguments.wasm) {
            // K/Wasm support ES modules only.
            configuration.moduleKind = ModuleKind.ES
        }

        configuration.incrementalDataProvider = services[IncrementalDataProvider::class.java]
        configuration.incrementalResultsConsumer = services[IncrementalResultsConsumer::class.java]
        configuration.incrementalNextRoundChecker = services[IncrementalNextRoundChecker::class.java]
        configuration.lookupTracker = services[LookupTracker::class.java]
        configuration.expectActualTracker = services[ExpectActualTracker::class.java]

        val sourceMapEmbedContentString = arguments.sourceMapEmbedSources
        var sourceMapContentEmbedding: SourceMapSourceEmbedding? = if (sourceMapEmbedContentString != null) {
            sourceMapContentEmbeddingMap[sourceMapEmbedContentString]
        } else {
            SourceMapSourceEmbedding.INLINING
        }
        if (sourceMapContentEmbedding == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map source embedding mode: $sourceMapEmbedContentString. Valid values are: ${sourceMapContentEmbeddingMap.keys.joinToString()}"
            )
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING
        }
        configuration.sourceMapEmbedSources = sourceMapContentEmbedding
        configuration.sourceMapIncludeMappingsFromUnavailableFiles = arguments.includeUnavailableSourcesIntoSourceMap

        if (!arguments.sourceMap && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null)
        }

        val sourceMapNamesPolicyString = arguments.sourceMapNamesPolicy
        var sourceMapNamesPolicy: SourceMapNamesPolicy? = if (sourceMapNamesPolicyString != null) {
            sourceMapNamesPolicyMap[sourceMapNamesPolicyString]
        } else {
            SourceMapNamesPolicy.SIMPLE_NAMES
        }
        if (sourceMapNamesPolicy == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map names policy: $sourceMapNamesPolicyString. Valid values are: ${sourceMapNamesPolicyMap.keys.joinToString()}"
            )
            sourceMapNamesPolicy = SourceMapNamesPolicy.SIMPLE_NAMES
        }
        configuration.sourcemapNamesPolicy = sourceMapNamesPolicy

        configuration.printReachabilityInfo = arguments.irDcePrintReachabilityInfo
        configuration.fakeOverrideValidator = arguments.fakeOverrideValidator
        configuration.dumpReachabilityInfoToFile = arguments.irDceDumpReachabilityInfoToFile

        configuration.setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage = arguments.includes != null, // no PL when producing KLIB
            onWarning = { messageCollector.report(WARNING, it) },
            onError = { messageCollector.report(ERROR, it) }
        )
    }

    internal fun initializeCommonConfiguration(configuration: CompilerConfiguration, arguments: K2JSCompilerArguments) {
        val libraries: List<String> = configureLibraries(arguments.libraries) + listOfNotNull(arguments.includes)
        val friendLibraries: List<String> = configureLibraries(arguments.friendModules)
        configuration.libraries += libraries
        configuration.friendLibraries += friendLibraries
        configuration.transitiveLibraries += libraries
        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.hmppModuleStructure
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg), hmppCliModuleStructure?.getModuleNameForSource(arg))
        }

        arguments.relativePathBases?.let {
            configuration.klibRelativePathBases += it
        }
        configuration.klibNormalizeAbsolutePath = arguments.normalizeAbsolutePath
        configuration.produceKlibSignaturesClashChecks = arguments.enableSignatureClashChecks

        configuration.noDoubleInlining = arguments.noDoubleInlining
        configuration.duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.parseOrDefault(
            arguments.duplicatedUniqueNameStrategy,
            default = DuplicatedUniqueNameStrategy.DENY
        )
        val moduleName = arguments.irModuleName ?: arguments.moduleName!!
        configuration.moduleName = moduleName
        configuration.allowKotlinPackage = arguments.allowKotlinPackage
        configuration.renderDiagnosticInternalName = arguments.renderInternalDiagnosticNames
    }

    private fun checkOutputArguments(arguments: K2JSCompilerArguments, messageCollector: MessageCollector) {
        if (arguments.outputDir == null) {
            messageCollector.report(ERROR, "IR: Specify output dir via -ir-output-dir", location = null)
        }

        if (arguments.moduleName == null) {
            messageCollector.report(ERROR, "IR: Specify output name via -ir-output-name", location = null)
        }
    }
}
