/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.*

open class UserVisibleIrModulesSupport(externalDependenciesLoader: ExternalDependenciesLoader) {
    /**
     * Load external [ResolvedDependency]s provided by the build system. These dependencies:
     * - all have [ResolvedDependency.selectedVersion] specified
     * - keep the information about which modules are first-level dependencies (i.e. the modules that the source code module
     *   directly depends on) and indirect (transitive) dependencies
     * - miss modules provided by Kotlin/Native distribution (stdlib, endorsed and platform libraries), as they are
     *   not visible to the build system
     */
    interface ExternalDependenciesLoader {
        fun load(): ResolvedDependencies

        companion object {
            val EMPTY = object : ExternalDependenciesLoader {
                override fun load(): ResolvedDependencies = ResolvedDependencies.EMPTY
            }

            fun from(externalDependenciesFile: File?, onMalformedExternalDependencies: (String) -> Unit): ExternalDependenciesLoader =
                if (externalDependenciesFile != null)
                    object : ExternalDependenciesLoader {
                        override fun load(): ResolvedDependencies {
                            return if (externalDependenciesFile.exists) {
                                // Deserialize external dependencies from the [externalDependenciesFile].
                                val externalDependenciesText = String(externalDependenciesFile.readBytes())
                                ResolvedDependenciesSupport.deserialize(externalDependenciesText) { lineNo, line ->
                                    onMalformedExternalDependencies("Malformed external dependencies at $externalDependenciesFile:$lineNo: $line")
                                }
                            } else ResolvedDependencies.EMPTY
                        }
                    }
                else
                    EMPTY
        }
    }

    private val externalDependencies: ResolvedDependencies by lazy {
        externalDependenciesLoader.load()
    }

    private val externalDependencyModules: Collection<ResolvedDependency>
        get() = externalDependencies.modules

    val sourceCodeModuleId: ResolvedDependencyId
        get() = externalDependencies.sourceCodeModuleId

    fun getUserVisibleModuleId(deserializer: IrModuleDeserializer): ResolvedDependencyId {
        val nameFromMetadataModuleHeader: String = deserializer.moduleFragment.name.asStringStripSpecialMarkers()
        val nameFromKlibManifest: String? = deserializer.asDeserializedKotlinLibrary?.uniqueName

        return ResolvedDependencyId(listOfNotNull(nameFromMetadataModuleHeader, nameFromKlibManifest))
    }

    open fun getUserVisibleModules(deserializers: Collection<IrModuleDeserializer>): Map<ResolvedDependencyId, ResolvedDependency> {
        return mergedModules(deserializers)
    }

    /**
     * Load [ResolvedDependency]s that represent all libraries participating in the compilation. Includes external dependencies,
     * but without version and hierarchy information. Also includes the libraries that are not visible to the build system
     * (and therefore are missing in [ExternalDependenciesLoader.load]) but are provided by the compiler. For Kotlin/Native such
     * libraries are stdlib, endorsed and platform libraries.
     */
    protected open fun modulesFromDeserializers(
        deserializers: Collection<IrModuleDeserializer>,
        excludedModuleIds: Set<ResolvedDependencyId>
    ): Map<ResolvedDependencyId, ResolvedDependency> {
        val modules: Map<ResolvedDependencyId, ModuleWithUninitializedDependencies> = deserializers.mapNotNull { deserializer ->
            val moduleId = getUserVisibleModuleId(deserializer)
            if (moduleId in excludedModuleIds) return@mapNotNull null

            val module = ResolvedDependency(
                id = moduleId,
                // TODO: support extracting all the necessary details for non-Native libs: selectedVersion, requestedVersions, artifacts
                selectedVersion = ResolvedDependencyVersion.EMPTY,
                // Assumption: As we don't know for sure which modules the source code module depends on directly and which modules
                // it depends on transitively, so let's assume it depends on all modules directly.
                requestedVersionsByIncomingDependencies = mutableMapOf(
                    ResolvedDependencyId.DEFAULT_SOURCE_CODE_MODULE_ID to ResolvedDependencyVersion.EMPTY
                ),
                artifactPaths = mutableSetOf()
            )

            val outgoingDependencyIds = deserializer.moduleDependencies.map { getUserVisibleModuleId(it) }

            moduleId to ModuleWithUninitializedDependencies(module, outgoingDependencyIds)
        }.toMap()

        // Stamp dependencies.
        return modules.stampDependenciesWithRequestedVersionEqualToSelectedVersion()
    }

    /**
     * The result of the merge of [ExternalDependenciesLoader.load] and [modulesFromDeserializers].
     */
    protected fun mergedModules(deserializers: Collection<IrModuleDeserializer>): MutableMap<ResolvedDependencyId, ResolvedDependency> {
        val externalDependencyModulesByNames: Map</* unique name */ String, ResolvedDependency> =
            mutableMapOf<String, ResolvedDependency>().apply {
                externalDependencyModules.forEach { externalDependency ->
                    externalDependency.id.uniqueNames.forEach { uniqueName ->
                        this[uniqueName] = externalDependency
                    }
                }
            }

        fun findMatchingExternalDependencyModule(moduleId: ResolvedDependencyId): ResolvedDependency? =
            moduleId.uniqueNames.firstNotNullOfOrNull { uniqueName -> externalDependencyModulesByNames[uniqueName] }

        // The build system may express a group of modules where one module is a library KLIB and one or more modules
        // are just C-interop KLIBs as a single module with multiple artifacts. We need to expand them so that every particular
        // module/artifact will be represented as an individual [ResolvedDependency] instance.
        val artifactPathsToOriginModules: MutableMap<ResolvedDependencyArtifactPath, ResolvedDependency> = mutableMapOf()
        externalDependencyModules.forEach { originModule ->
            originModule.artifactPaths.forEach { artifactPath -> artifactPathsToOriginModules[artifactPath] = originModule }
        }

        // Collect "provided" modules, i.e. modules that are missing in "external dependencies".
        val providedModules = mutableListOf<ResolvedDependency>()

        // Next, merge external dependencies with dependencies from deserializers.
        modulesFromDeserializers(
            deserializers = deserializers,
            excludedModuleIds = setOf(sourceCodeModuleId)
        ).forEach { (moduleId, module) ->
            val externalDependencyModule = findMatchingExternalDependencyModule(moduleId)
            if (externalDependencyModule != null) {
                // Just add missing dependencies to the same module in [mergedModules].
                module.requestedVersionsByIncomingDependencies.forEach { (incomingDependencyId, requestedVersion) ->
                    val adjustedIncomingDependencyId = findMatchingExternalDependencyModule(incomingDependencyId)?.id
                        ?: incomingDependencyId
                    if (adjustedIncomingDependencyId !in externalDependencyModule.requestedVersionsByIncomingDependencies) {
                        externalDependencyModule.requestedVersionsByIncomingDependencies[adjustedIncomingDependencyId] = requestedVersion
                    }
                }
            } else {
                val originModuleVersion = module.artifactPaths.firstNotNullOfOrNull { artifactPathsToOriginModules[it] }?.selectedVersion
                if (originModuleVersion != null) {
                    // Handle artifacts that needs to be represented as individual [ResolvedDependency] objects.
                    module.selectedVersion = originModuleVersion

                    val incomingDependencyIdsToStampRequestedVersion = module.requestedVersionsByIncomingDependencies
                        .mapNotNull { (incomingDependencyId, requestedVersion) ->
                            if (requestedVersion.isEmpty()) incomingDependencyId else null
                        }
                    incomingDependencyIdsToStampRequestedVersion.forEach { incomingDependencyId ->
                        module.requestedVersionsByIncomingDependencies[incomingDependencyId] = originModuleVersion
                    }
                } else {
                    // Just keep the module as is. If it has no incoming dependencies, then treat it as
                    // the first-level dependency module (i.e. the module that only the source code module depends on).
                    if (module.requestedVersionsByIncomingDependencies.isEmpty()) {
                        module.requestedVersionsByIncomingDependencies[sourceCodeModuleId] = module.selectedVersion
                    }
                }

                // Patch incoming dependencies.
                module.requestedVersionsByIncomingDependencies.mapNotNull { (incomingDependencyId, requestedVersion) ->
                    val adjustedIncomingDependencyId = findMatchingExternalDependencyModule(incomingDependencyId)?.id
                        ?: return@mapNotNull null
                    Triple(incomingDependencyId, adjustedIncomingDependencyId, requestedVersion)
                }.forEach { (incomingDependencyId, adjustedIncomingDependencyId, requestedVersion) ->
                    module.requestedVersionsByIncomingDependencies.remove(incomingDependencyId)
                    module.requestedVersionsByIncomingDependencies[adjustedIncomingDependencyId] = requestedVersion
                }

                providedModules += module
            }
        }

        return (externalDependencyModules + providedModules).associateByTo(mutableMapOf()) { it.id }
    }

    protected data class ModuleWithUninitializedDependencies(
        val module: ResolvedDependency,
        val outgoingDependencyIds: List<ResolvedDependencyId>
    )

    private fun Map<ResolvedDependencyId, ModuleWithUninitializedDependencies>.stampDependenciesWithRequestedVersionEqualToSelectedVersion(): Map<ResolvedDependencyId, ResolvedDependency> {
        return mapValues { (moduleId, moduleWithUninitializedDependencies) ->
            val (module, outgoingDependencyIds) = moduleWithUninitializedDependencies
            outgoingDependencyIds.forEach { outgoingDependencyId ->
                val dependencyModule = getValue(outgoingDependencyId).module
                dependencyModule.requestedVersionsByIncomingDependencies[moduleId] = dependencyModule.selectedVersion
            }
            module
        }
    }

    protected val IrModuleDeserializer.asDeserializedKotlinLibrary: KotlinLibrary?
        get() = if (kind == IrModuleDeserializerKind.DESERIALIZED) klib as? KotlinLibrary else null

    val moduleIdComparator: Comparator<ResolvedDependencyId> = Comparator { a, b ->
        when {
            a == b -> 0
            // Kotlin libs go lower.
            a.isKotlinLibrary && !b.isKotlinLibrary -> 1
            !a.isKotlinLibrary && b.isKotlinLibrary -> -1
            // Modules with simple names go upper as they are most likely user-made libs.
            a.hasSimpleName && !b.hasSimpleName -> -1
            !a.hasSimpleName && b.hasSimpleName -> 1
            // Else: just compare by names.
            else -> {
                val aUniqueNames = a.uniqueNames.iterator()
                val bUniqueNames = b.uniqueNames.iterator()

                while (aUniqueNames.hasNext() && bUniqueNames.hasNext()) {
                    val diff = aUniqueNames.next().compareTo(bUniqueNames.next())
                    if (diff != 0) return@Comparator diff
                }

                when {
                    aUniqueNames.hasNext() -> 1
                    bUniqueNames.hasNext() -> -1
                    else -> 0
                }
            }
        }
    }

    protected open val ResolvedDependencyId.isKotlinLibrary: Boolean
        get() = uniqueNames.any { uniqueName -> uniqueName.startsWith(KOTLIN_LIBRARY_PREFIX) }

    protected open val ResolvedDependencyId.hasSimpleName: Boolean
        get() = uniqueNames.all { uniqueName -> uniqueName.none { it == '.' || it == ':' } }

    companion object {
        const val KOTLIN_LIBRARY_PREFIX = "org.jetbrains.kotlin"

        val DEFAULT = UserVisibleIrModulesSupport(ExternalDependenciesLoader.EMPTY)
    }
}
