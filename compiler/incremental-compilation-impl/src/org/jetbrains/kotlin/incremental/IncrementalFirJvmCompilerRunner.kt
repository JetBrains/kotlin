/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.phaser.JvmPhaseCoordinator
import org.jetbrains.kotlin.backend.common.serialization.toIoFileOrNull
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationUpdater.firRunnerHack_setupModuleChunk
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import java.io.File

/**
 * The original FirRunner reused low-ish level details of Kotlin compiler to optimize scenarios
 * where compilation scope can be safely expanded via Compiler Frontend rounds
 *
 * This version removes most optimizations, but heavily reuses the existing pipeline phases
 * and allows for dirty set expansion between repeated frontend phases
 *
 * This is subject to discussion/design, one hypothesis is that FirRunner is useful for first round failures
 * so they should be supported first and then optimization options should be considered
 *
 * Also, current implementation is very close to the "mainline" IncrementalJvmCompilerRunner,
 * so there might be hope to fully switch to the FirJvm version (?) //TODO(emazhukin) speculation, citation needed, leading
 *
 * //TODO(emazhukin) what about the loss of `incrementalExcludesScope`, are they super important or not quite?
 */
open class IncrementalFirJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    outputDirs: Collection<File>?,
    classpathChanges: ClasspathChanges,
    kotlinSourceFilesExtensions: Set<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) : IncrementalJvmCompilerRunner(
    workingDir,
    reporter,
    outputDirs,
    classpathChanges,
    kotlinSourceFilesExtensions,
    icFeatures,
) {
    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JVMCompilerArguments,
        caches: IncrementalJvmCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
        val compiler = when {
            !isIncremental -> K2JVMCompiler()
            // in a non-incremental build, fir runner can't add any value
            // an edge case is when a frontend compiler plugin generates symbols that should be used in compilation of this module
            // but that sounds extremely ill-defined (and it would be caught by the outer ic loop) (right?)
            else -> object : K2JVMCompiler() {
                override fun doExecutePhased(
                    arguments: K2JVMCompilerArguments,
                    services: Services,
                    basicMessageCollector: MessageCollector
                ): ExitCode {
                    return JvmCliPipeline(
                        defaultPerformanceManager,
                        FirRunnerPhaseCoordinator(caches, args)
                    ).execute(arguments, services, basicMessageCollector)
                }
            }
        }
        val freeArgsBackup = args.freeArgs.toList()
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.allowNoSourceFiles = true
        val exitCode = compiler.exec(messageCollector, services, args)
        // note that inside `exec` phase coordinator can further add sour
        //TODO(emazhukin) consider catching PipelineStepException to deal with frontend errors by expanding the dirty set
        val actualSourcesToCompile = if (args.freeArgs.size - sourcesToCompile.size == freeArgsBackup.size) {
            sourcesToCompile // i.e. the compiler did not expand the dirty set during execution
        } else {
            args.freeArgs.subList(freeArgsBackup.size, args.freeArgs.size).map { File(it) }
        }
        args.freeArgs = freeArgsBackup
        reporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats) //TODO(emazhukin) some metrics also can be inaccurate in firrunner, need to check
        return exitCode to actualSourcesToCompile
    }

    private inner class FirRunnerPhaseCoordinator(
        val caches: IncrementalJvmCachesManager,
        val mutableArgs: K2JVMCompilerArguments
    ) : JvmPhaseCoordinator<PipelineContext> {
        private val alreadyCompiledByFrontendSources = mutableSetOf<File>()

        override fun nextPhaseOrEnd(
            lastPhase: CompilerPhase<PipelineContext, Any?, Any?>?,
            mutableInput: Any?,
            phaseOutput: Any?
        ): CompilerPhase<PipelineContext, Any?, Any?>? {
            @Suppress("UNCHECKED_CAST")
            return when (lastPhase) {
                null -> JvmConfigurationPipelinePhase
                is JvmConfigurationPipelinePhase -> {
                    // TODO if we need to back something up before the first frontend launch,
                    // do it here
                    JvmFrontendPipelinePhase
                }
                is JvmFrontendPipelinePhase -> {
                    // this branch means that we've run the frontend once, and now we need to either move on to the next phase,
                    // or run frontend again with a different dirtySet

                    // the logic corresponds to the `firIncrementalCycle()` in the original FirRunner

                    //TODO!!!!!!!!!!!(emazhukin) must log the fir specific logic to understand what's going on there
                    val firPipelineArtifact = phaseOutput as? JvmFrontendPipelineArtifact ?: return null // TODO check - error exit should look ok
                    alreadyCompiledByFrontendSources.addAll(firPipelineArtifact.sourceFiles.mapNotNull { it.toIoFileOrNull() })
                    val expandedDirtySources = collectNewDirtySourcesFromFirResult(
                        firPipelineArtifact, caches, alreadyCompiledByFrontendSources,
                        reporter,
                    )
                    // note: it's always correct to pass alreadyCompiledByFrontendSources as excludes
                    // with monotonous dirtySet, if no new files are added to dirtySet, it's the final round
                    // and with non-monotonous dirtySet, this risky optimization is "in the name"


                    //println("==============")
                    //println("compiled so far: ${alreadyCompiledByFrontendSources.joinToString(", ") { it.name }}")
                    //println("new dirty sources: ${expandedDirtySources.joinToString(", ") { it.name }}")

                    //TODO(emazhukin) is frontend dirtyset allowed to not be monotonous? i.e. can we run frontend once per file max?
                    // i bet we can not
                    val dirtySetDiff = expandedDirtySources - alreadyCompiledByFrontendSources

                    //println("sanity check - dsetdiff: ${expandedDirtySources.joinToString(", ") { it.name }}")
                    //println("==============")

                    when (dirtySetDiff.size) {
                        0 -> JvmFir2IrPipelinePhase
                        else -> {
                            val effectiveDirtySet = when (icFeatures.enableMonotonousIncrementalCompileSetExpansion) {
                                true -> alreadyCompiledByFrontendSources + dirtySetDiff
                                false -> dirtySetDiff
                            }
                            caches.platformCache.markDirty(effectiveDirtySet)
                            caches.inputsCache.removeOutputForSourceFiles(effectiveDirtySet)
                            firPipelineArtifact.environment.localFileSystem.refresh(false) //TODO(emazhukin) why is this required?

                            // TODO(emazhukin) two options here:
                            // A: add new dirty sources as freeArg, go back to phase 0 - safe & inefficient
                            // B: add new dirty sources as freeArg, then force configuration updater to `setupModuleChunk`
                            //    looks like it's the only usage of sources-to-compile in compiler, but idk

                            mutableArgs.freeArgs += dirtySetDiff.map { it.absolutePath }
                            val frontendInput = mutableInput as? ConfigurationPipelineArtifact
                                ?: error("unexpected branch: frontend was ran with wrong input ${mutableInput!!::class.java.name}")
                            frontendInput.configuration.firRunnerHack_setupModuleChunk(mutableArgs)

                            JvmFrontendPipelinePhase
                        }
                    }
                }
                is JvmFir2IrPipelinePhase -> JvmBackendPipelinePhase
                is JvmBackendPipelinePhase -> JvmWriteOutputsPhase
                is JvmWriteOutputsPhase -> null
                else -> error("unexpected branch: unknown jvm pipeline phase")
            } as CompilerPhase<PipelineContext, Any?, Any?>?
        }
    }
}
