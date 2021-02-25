/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.compiler.wjs

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.DFS
import java.io.File

class ModuleLoader(
    private val resolvedLibraries: KotlinLibraryResolveResult,
    private val descriptorFactory: KlibMetadataModuleDescriptorFactory,
    private val configuration: CompilerConfiguration,
    private val storageManager: LockBasedStorageManager
) {

    private val lookupTracker: LookupTracker =
        configuration[CommonConfigurationKeys.LOOKUP_TRACKER] ?: LookupTracker.DO_NOTHING

    var builtInsModule: ModuleDescriptorImpl? = null
        private set

    private val resolvedDependenciesRpo: List<KotlinResolvedLibrary> = computeDependenciesRpo()

    val dependenciesRpo: List<KotlinLibrary> = resolvedDependenciesRpo.map { it.library }

    private fun computeDependenciesRpo(): List<KotlinResolvedLibrary> {
        val transitives = resolvedLibraries.getFullResolvedList()
        return DFS.topologicalOrder(transitives) { it.resolvedDependencies }.reversed()
    }

    val dependencyDescriptors: Map<String, ModuleDescriptorImpl> = loadDependencies()

    private fun loadDependencies(): Map<String, ModuleDescriptorImpl> {
        val depsMap: MutableMap<String, ModuleDescriptorImpl> = mutableMapOf()

        fun loadSingleModule(current: KotlinResolvedLibrary): ModuleDescriptorImpl {
            val library = current.library
            return depsMap.getOrPut(library.libraryName) {
                descriptorFactory.createDescriptorOptionalBuiltIns(
                    library,
                    configuration.languageVersionSettings,
                    storageManager,
                    builtInsModule?.builtIns,
                    packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
                    lookupTracker = lookupTracker
                ).apply {
                    val klibDependencies = current.resolvedDependencies
                    if (klibDependencies.isEmpty()) builtInsModule = this
                    val dependencies = ArrayList<ModuleDescriptorImpl>(klibDependencies.size + 1)
                    dependencies.add(this)
                    klibDependencies.mapTo(dependencies) { loadSingleModule(it) }
                    setDependencies(dependencies)
                }
            }
        }

        resolvedDependenciesRpo.forEach { loadSingleModule(it) }

        return depsMap
    }

    fun resolveModuleByPath(path: String): KotlinLibrary? {
        val canonicalPath = File(path).canonicalPath
        return dependenciesRpo.singleOrNull { it.libraryFile.canonicalPath == canonicalPath }
    }

    companion object {
        val jsMetadataFactories = KlibMetadataFactories({ object : KotlinBuiltIns(it) {} }, DynamicTypeDeserializer)
    }
}