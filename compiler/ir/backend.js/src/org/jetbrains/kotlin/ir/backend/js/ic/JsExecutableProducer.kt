/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.serialization.js.ModuleKind

class JsExecutableProducer(
    private val mainModuleName: String,
    private val moduleKind: ModuleKind,
    private val sourceMapsInfo: SourceMapsInfo?,
    private val caches: List<ModuleArtifact>,
    private val relativeRequirePath: Boolean
) {
    fun buildExecutable(multiModule: Boolean, rebuildCallback: (String) -> Unit = {}) = if (multiModule) {
        buildMultiModuleExecutable(rebuildCallback)
    } else {
        buildSingleModuleExecutable(rebuildCallback)
    }

    private fun buildSingleModuleExecutable(rebuildCallback: (String) -> Unit): CompilationOutputs {
        val program = JsIrProgram(caches.map { cacheArtifact -> cacheArtifact.loadJsIrModule() })
        val out = generateSingleWrappedModuleBody(
            moduleName = mainModuleName,
            moduleKind = moduleKind,
            fragments = program.modules.flatMap { it.fragments },
            sourceMapsInfo = sourceMapsInfo,
            generateScriptModule = false,
            generateCallToMain = true
        )
        rebuildCallback(mainModuleName)
        return out
    }

    private fun buildMultiModuleExecutable(rebuildCallback: (String) -> Unit): CompilationOutputs {
        val jsMultiModuleCache = JsMultiModuleCache(caches)
        val cachedProgram = jsMultiModuleCache.loadProgramHeadersFromCache()

        val resolver = CrossModuleDependenciesResolver(cachedProgram.map { it.jsIrHeader })
        val crossModuleReferences = resolver.resolveCrossModuleDependencies(relativeRequirePath)

        jsMultiModuleCache.loadRequiredJsIrModules(crossModuleReferences)

        fun JsMultiModuleCache.CachedModuleInfo.compileModule(moduleName: String, generateCallToMain: Boolean): CompilationOutputs {
            if (jsIrHeader.associatedModule == null) {
                val compilationOutputs = jsMultiModuleCache.fetchCompiledJsCode(artifact)
                if (compilationOutputs != null) {
                    return compilationOutputs
                }
                jsIrHeader.associatedModule = artifact.loadJsIrModule()
            }
            val associatedModule = jsIrHeader.associatedModule ?: error("Internal error: cannot load module $moduleName")
            val crossRef = crossModuleReferences[jsIrHeader] ?: error("Internal error: cannot find cross references for module $moduleName")
            crossRef.initJsImportsForModule(associatedModule)

            val compiledModule = generateSingleWrappedModuleBody(
                moduleName = moduleName,
                moduleKind = moduleKind,
                associatedModule.fragments,
                sourceMapsInfo = sourceMapsInfo,
                generateScriptModule = false,
                generateCallToMain = generateCallToMain,
                crossModuleReferences = crossRef
            )
            jsMultiModuleCache.commitCompiledJsCode(artifact, compiledModule)
            rebuildCallback(moduleName)
            return compiledModule
        }

        val cachedMainModule = cachedProgram.last()
        val mainModule = cachedMainModule.compileModule(mainModuleName, true)

        val cachedOtherModules = cachedProgram.dropLast(1)
        val dependencies = cachedOtherModules.map {
            it.jsIrHeader.externalModuleName to it.compileModule(it.jsIrHeader.externalModuleName, false)
        }
        return CompilationOutputs(mainModule.jsCode, mainModule.jsProgram, mainModule.sourceMap, dependencies)
    }
}
