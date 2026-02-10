/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsCached
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleDependenciesResolver
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.generateSingleWrappedModuleBody
import org.jetbrains.kotlin.ir.backend.js.tsexport.TypeScriptDefinitionsFragment
import org.jetbrains.kotlin.ir.backend.js.tsexport.TypeScriptMerger
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * This class is responsible for incrementally producing the final JavaScript code based on the provided cache artifacts.
 * @param caches - Cache artifacts, which are instances of [JsModuleArtifact], can be obtained using [CacheUpdater.actualizeCaches].
 */
class JsExecutableProducer(
    private val mainModuleName: String,
    private val moduleKind: ModuleKind,
    private val sourceMapsInfo: SourceMapsInfo?,
    private val dtsCompilationStrategy: TsCompilationStrategy,
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
            JsGenerationGranularity.PER_FILE -> buildMultiArtifactExecutable(outJsProgram, JsPerFileCache(moduleKind, caches))
        }

    private fun buildSingleModuleExecutable(outJsProgram: Boolean): BuildResult {
        val modules = caches.map { cacheArtifact -> cacheArtifact.loadJsIrModule() }
        val fragments = modules.flatMap { it.fragments }
        val out = generateSingleWrappedModuleBody(
            moduleName = mainModuleName,
            moduleKind = moduleKind,
            fragments = fragments,
            sourceMapsInfo = sourceMapsInfo,
            generateCallToMain = true,
            outJsProgram = outJsProgram,
            typeScriptDefinitions = runIf(dtsCompilationStrategy != TsCompilationStrategy.NONE) {
                val tsFragments = fragments.mapNotNull { it.dts }.ifEmpty { return@runIf null }
                TypeScriptMerger(moduleKind).mergeIntoTypeScriptDefinitions(mainModuleName, tsFragments)
            }
        )
        return BuildResult(out, listOf(mainModuleName))
    }

    private fun <CacheInfo : JsMultiArtifactCache.CacheInfo> buildMultiArtifactExecutable(
        outJsProgram: Boolean,
        jsMultiArtifactCache: JsMultiArtifactCache<CacheInfo>
    ): BuildResult {
        val tsMerger = TypeScriptMerger(moduleKind)
        val noTypeScript = dtsCompilationStrategy == TsCompilationStrategy.NONE
        val generateTypeScriptPerArtifact = dtsCompilationStrategy == TsCompilationStrategy.PER_ARTIFACT

        val rebuildModules = mutableListOf<String>()
        stopwatch.startNext("JS code cache loading")
        val cachedProgram = jsMultiArtifactCache.loadProgramHeadersFromCache()

        stopwatch.startNext("Cross module references resolving")
        val resolver = CrossModuleDependenciesResolver(moduleKind, cachedProgram.map { it.jsIrHeader })
        val crossModuleReferences = resolver.resolveCrossModuleDependencies(relativeRequirePath)
        val mainModuleTsFragments = mutableListOf<TypeScriptDefinitionsFragment>()

        stopwatch.startNext("Loading JS IR modules with updated cross module references")
        jsMultiArtifactCache.loadRequiredJsIrModules(crossModuleReferences)

        fun CacheInfo.compileModule(moduleName: String, isMainModule: Boolean): CompilationOutputs {
            var mergedTsFragment: TypeScriptDefinitionsFragment? = null
            var cachedCompilationOutputs: CompilationOutputsCached? = null

            if (jsIrHeader.associatedModule == null || jsMultiArtifactCache.commitOnyTypeScriptFiles(this)) {
                stopwatch.startNext("Fetching cached JS code")
                val compilationOutputs = jsMultiArtifactCache.fetchCompiledJsCode(this)

                mergedTsFragment = runIf(!noTypeScript) {
                    stopwatch.startNext("Loading cached TypeScript fragment")
                    jsMultiArtifactCache.loadTypeScriptFragment(this)
                }

                if (compilationOutputs != null) {
                    cachedCompilationOutputs = compilationOutputs
                } else {
                    // theoretically should never happen
                    stopwatch.startNext("Loading JS IR modules")
                    jsIrHeader.associatedModule = jsMultiArtifactCache.loadJsIrModule(this)
                }
            }

            if (!noTypeScript && mergedTsFragment == null) {
                val tsFragments = jsIrHeader.associatedModule?.fragments?.mapNotNull { it.dts }?.ifEmpty { null }
                mergedTsFragment = tsFragments?.let(tsMerger::mergeIntoSingleFragment)
                stopwatch.startNext("Committing module TypeScript fragment")
                jsMultiArtifactCache.commitTypeScriptFragment(this, mergedTsFragment)
            }

            val tsDefinitions = mergedTsFragment?.let {
                when {
                    generateTypeScriptPerArtifact -> tsMerger.generateSingleWrappedTypeScriptDefinitions(moduleName, mergedTsFragment)
                    isMainModule -> tsMerger.mergeIntoTypeScriptDefinitions(moduleName, mainModuleTsFragments + mergedTsFragment)
                    else -> null.also { mainModuleTsFragments.add(mergedTsFragment) }
                }
            }

            cachedCompilationOutputs?.let {
                it.tsDefinitions = tsDefinitions
                return it
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
                outJsProgram = outJsProgram,
                typeScriptDefinitions = tsDefinitions,
            )

            stopwatch.startNext("Committing compiled JS code")
            rebuildModules += moduleName
            return jsMultiArtifactCache.commitCompiledJsCode(this, compiledModule)
        }

        val cachedMainModule = cachedProgram.last()
        val cachedOtherModules = cachedProgram.dropLast(1)

        val dependencies = cachedOtherModules.map {
            it.jsIrHeader.externalModuleName to it.compileModule(it.jsIrHeader.externalModuleName, false)
        }

        val mainModuleCompilationOutput = cachedMainModule
            .compileModule(mainModuleName, true)
            .also { it.dependencies += dependencies }

        stopwatch.stop()
        return BuildResult(mainModuleCompilationOutput, rebuildModules)
    }
}
