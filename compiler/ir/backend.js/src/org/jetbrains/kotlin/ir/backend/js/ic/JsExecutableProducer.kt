/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.serialization.js.ModuleKind

/**
 * This class is responsible for incrementally producing the final JavaScript code based on the provided cache artifacts.
 * @param caches - Cache artifacts, which are instances of [JsModuleArtifact], can be obtained using [CacheUpdater.actualizeCaches].
 */
class JsExecutableProducer(
    private val mainModuleName: String,
    private val moduleKind: ModuleKind,
    private val sourceMapsInfo: SourceMapsInfo?,
    private val caches: List<JsModuleArtifact>,
    private val relativeRequirePath: Boolean
) {
    data class BuildResult(val compilationOut: CompilationOutputs, val buildModules: List<String>)

    private val stopwatch = StopwatchIC()

    fun getStopwatchLaps() = buildMap {
        stopwatch.laps.forEach {
            this[it.first] = it.second + (this[it.first] ?: 0L)
        }
    }

    fun buildExecutable(granularity: JsGenerationGranularity, outJsProgram: Boolean) =
        when (granularity) {
            JsGenerationGranularity.WHOLE_PROGRAM -> buildSingleModuleExecutable(outJsProgram)
            JsGenerationGranularity.PER_MODULE -> buildMultiArtifactExecutable(outJsProgram, JsPerModuleCache(moduleKind, caches))
            JsGenerationGranularity.PER_FILE -> buildMultiArtifactExecutable(outJsProgram, JsPerFileCache(caches))
        }

    private fun buildSingleModuleExecutable(outJsProgram: Boolean): BuildResult {
        val modules = caches.map { cacheArtifact -> cacheArtifact.loadJsIrModule() }
        val out = generateSingleWrappedModuleBody(
            moduleName = mainModuleName,
            moduleKind = moduleKind,
            fragments = modules.flatMap { it.fragments },
            sourceMapsInfo = sourceMapsInfo,
            generateCallToMain = true,
            outJsProgram = outJsProgram
        )
        return BuildResult(out, listOf(mainModuleName))
    }

    private fun <CacheInfo : JsMultiArtifactCache.CacheInfo> buildMultiArtifactExecutable(
        outJsProgram: Boolean,
        jsMultiArtifactCache: JsMultiArtifactCache<CacheInfo>
    ): BuildResult {
        val rebuildModules = mutableListOf<String>()
        stopwatch.startNext("JS code cache loading")
        val cachedProgram = jsMultiArtifactCache.loadProgramHeadersFromCache()

        stopwatch.startNext("Cross module references resolving")
        val resolver = CrossModuleDependenciesResolver(moduleKind, cachedProgram.map { it.jsIrHeader })
        val crossModuleReferences = resolver.resolveCrossModuleDependencies(relativeRequirePath)

        stopwatch.startNext("Loading JS IR modules with updated cross module references")
        jsMultiArtifactCache.loadRequiredJsIrModules(crossModuleReferences)

        fun CacheInfo.compileModule(moduleName: String, isMainModule: Boolean): CompilationOutputs {
            if (jsIrHeader.associatedModule == null) {
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
                moduleName = moduleName,
                moduleKind = moduleKind,
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
            .compileModule(mainModuleName, true)
            .apply {
                dependencies += cachedOtherModules.map {
                    it.jsIrHeader.externalModuleName to it.compileModule(it.jsIrHeader.externalModuleName, false)
                }
            }

        stopwatch.stop()
        return BuildResult(mainModuleCompilationOutput, rebuildModules)
    }
}
