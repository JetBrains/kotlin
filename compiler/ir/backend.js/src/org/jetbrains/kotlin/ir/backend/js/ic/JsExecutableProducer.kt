/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleDependenciesResolver
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.generateSingleWrappedModuleBody
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration

/**
 * This class is responsible for incrementally producing the final JavaScript code based on the provided cache artifacts.
 * @param caches - Cache artifacts, which are instances of [JsModuleArtifact], can be obtained using [CacheUpdater.actualizeCaches].
 */
class JsExecutableProducer(
    private val artifactConfiguration: WebArtifactConfiguration,
    private val sourceMapsInfo: SourceMapsInfo?,
    private val caches: List<JsModuleArtifact>,
) {
    data class BuildResult(val compilationOut: CompilationOutputs, val buildModules: List<String>)

    private val stopwatch = StopwatchIC()

    fun getStopwatchLaps() = buildMap {
        stopwatch.laps.forEach {
            this[it.first] = it.second + (this[it.first] ?: 0L)
        }
    }

    fun buildExecutable(outJsProgram: Boolean) = when (artifactConfiguration.granularity) {
        JsGenerationGranularity.WHOLE_PROGRAM -> buildSingleModuleExecutable(outJsProgram)
        JsGenerationGranularity.PER_MODULE -> buildMultiArtifactExecutable(
            outJsProgram,
            JsPerModuleCache(artifactConfiguration, caches)
        )
        JsGenerationGranularity.PER_FILE -> buildMultiArtifactExecutable(outJsProgram, JsPerFileCache(artifactConfiguration, caches))
    }

    private fun buildSingleModuleExecutable(outJsProgram: Boolean): BuildResult {
        val modules = caches.map { cacheArtifact -> cacheArtifact.loadJsIrModule() }
        val out = generateSingleWrappedModuleBody(
            artifactConfiguration,
            fragments = modules.flatMap { it.fragments },
            sourceMapsInfo = sourceMapsInfo,
            generateCallToMain = true,
            outJsProgram = outJsProgram
        )
        return BuildResult(out, listOf(artifactConfiguration.moduleName))
    }

    private fun <CacheInfo : JsMultiArtifactCache.CacheInfo> buildMultiArtifactExecutable(
        outJsProgram: Boolean,
        jsMultiArtifactCache: JsMultiArtifactCache<CacheInfo>
    ): BuildResult {
        val rebuildModules = mutableListOf<String>()
        stopwatch.startNext("JS code cache loading")
        val cachedProgram = jsMultiArtifactCache.loadProgramHeadersFromCache()

        stopwatch.startNext("Cross module references resolving")
        val resolver = CrossModuleDependenciesResolver(artifactConfiguration.moduleKind, cachedProgram.map { it.jsIrHeader })
        val crossModuleReferences = resolver.resolveCrossModuleDependencies()

        stopwatch.startNext("Loading JS IR modules with updated cross module references")
        jsMultiArtifactCache.loadRequiredJsIrModules(crossModuleReferences)

        fun CacheInfo.compileModule(moduleName: String, outputName: String, isMainModule: Boolean): CompilationOutputs {
            if (jsIrHeader.associatedModule == null || jsMultiArtifactCache.commitOnyTypeScriptFiles(this)) {
                stopwatch.startNext("Fetching cached JS code")
                val compilationOutputs = jsMultiArtifactCache.fetchCompiledJsCode(this)
                if (compilationOutputs != null) {
                    return compilationOutputs
                }
                // theoretically should never happen
                stopwatch.startNext("Loading JS IR modules")
                jsIrHeader.associatedModule = jsMultiArtifactCache.loadJsIrModule(this)
            }
            stopwatch.startNext("Initializing JS imports")
            val associatedModule = jsIrHeader.associatedModule ?: icError("can not load module $moduleName")
            val crossRef = crossModuleReferences[jsIrHeader] ?: icError("can not find cross references for module $moduleName")
            crossRef.initJsImportsForModule(associatedModule)

            stopwatch.startNext("Generating JS code")
            val compiledModule = generateSingleWrappedModuleBody(
                artifactConfiguration.copy(moduleName = moduleName, outputName = outputName),
                associatedModule.fragments,
                sourceMapsInfo = sourceMapsInfo,
                generateCallToMain = isMainModule,
                crossModuleReferences = crossRef,
                outJsProgram = outJsProgram
            )

            stopwatch.startNext("Committing compiled JS code")
            rebuildModules += moduleName
            return jsMultiArtifactCache.commitCompiledJsCode(this, compiledModule)
        }

        val cachedMainModule = cachedProgram.last()
        val cachedOtherModules = cachedProgram.dropLast(1)

        val mainModuleCompilationOutput = cachedMainModule
            .compileModule(artifactConfiguration.moduleName, artifactConfiguration.outputName, isMainModule = true)
            .apply {
                dependencies += cachedOtherModules.map {
                    it.compileModule(it.jsIrHeader.externalModuleName, it.jsIrHeader.externalModuleName, isMainModule = false)
                }
            }

        stopwatch.stop()
        return BuildResult(mainModuleCompilationOutput, rebuildModules)
    }
}
