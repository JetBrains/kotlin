/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logging

private val logger = Logging.getLogger(KotlinMPPGradleModelBuilder::class.java)

internal interface MultiplatformModelImportingContext {
    val project: Project

    val targets: Collection<KotlinTarget>
    val compilations: Collection<KotlinCompilation>

    /**
     * All source sets in a project, including those that are created but not included into any compilations
     * (so-called "orphan" source sets). Use [isOrphanSourceSet] to get only compiled source sets
     */
    val sourceSets: Collection<KotlinSourceSetImpl>
    val sourceSetsByNames: Map<String, KotlinSourceSetImpl>

    fun sourceSetByName(name: String): KotlinSourceSet?
    fun compilationsBySourceSet(sourceSet: KotlinSourceSet): Collection<KotlinCompilation>?

    /**
     * "Orphan" is a source set which is not actually compiled by the compiler, i.e. the one
     * which doesn't belong to any [KotlinCompilation].
     *
     * Orphan source sets might appear if one creates a source-set manually and doesn't link
     * it anywhere (essentially this is a misconfiguration)
     */
    fun isOrphanSourceSet(sourceSet: KotlinSourceSet): Boolean = compilationsBySourceSet(sourceSet) == null
}

internal fun MultiplatformModelImportingContext.getProperty(property: GradleImportProperties): Boolean = project.getProperty(property)

internal fun Project.getProperty(property: GradleImportProperties): Boolean {
    val explicitValueIfAny = try {
        (findProperty(property.id) as? String)?.toBoolean()
    } catch (e: Exception) {
        logger.error("Error while trying to read property $property from project $project", e)
        null
    }

    return explicitValueIfAny ?: property.defaultValue
}

internal enum class GradleImportProperties(val id: String, val defaultValue: Boolean) {
    IS_HMPP_ENABLED("kotlin.mpp.enableGranularSourceSetsMetadata", false),
    COERCE_ROOT_SOURCE_SETS_TO_COMMON("kotlin.mpp.coerceRootSourceSetsToCommon", true),
    ENABLE_NATIVE_DEPENDENCY_PROPAGATION("kotlin.native.enableDependencyPropagation", true),
    BUILD_METADATA_DEPENDENCIES("build_metadata_dependencies_for_actualised_source_sets", true),
    IMPORT_ORPHAN_SOURCE_SETS("import_orphan_source_sets", true),
    INCLUDE_ANDROID_DEPENDENCIES("kotlin.include.android.dependencies", false)
    ;
}


internal class MultiplatformModelImportingContextImpl(override val project: Project) : MultiplatformModelImportingContext {
    /** see [initializeSourceSets] */
    override lateinit var sourceSetsByNames: Map<String, KotlinSourceSetImpl>
        private set
    override val sourceSets: Collection<KotlinSourceSetImpl>
        get() = sourceSetsByNames.values

    /** see [initializeCompilations] */
    override lateinit var compilations: Collection<KotlinCompilation>
        private set
    private lateinit var sourceSetToParticipatedCompilations: Map<KotlinSourceSet, Set<KotlinCompilation>>


    /** see [initializeTargets] */
    override lateinit var targets: Collection<KotlinTarget>
        private set

    internal fun initializeSourceSets(sourceSetsByNames: Map<String, KotlinSourceSetImpl>) {
        require(!this::sourceSetsByNames.isInitialized) {
            "Attempt to re-initialize source sets for $this. Previous value: ${this.sourceSetsByNames}"
        }
        this.sourceSetsByNames = sourceSetsByNames
    }

    internal fun initializeCompilations(compilations: Collection<KotlinCompilation>) {
        require(!this::compilations.isInitialized) { "Attempt to re-initialize compilations for $this. Previous value: ${this.compilations}" }
        this.compilations = compilations

        val sourceSetToCompilations = LinkedHashMap<KotlinSourceSet, MutableSet<KotlinCompilation>>()

        for (target in targets) {
            for (compilation in target.compilations) {
                for (sourceSet in compilation.sourceSets) {
                    sourceSetToCompilations.getOrPut(sourceSet) { LinkedHashSet() } += compilation
                    sourceSet.dependsOnSourceSets.mapNotNull { sourceSetByName(it) }.forEach {
                        sourceSetToCompilations.getOrPut(it) { LinkedHashSet() } += compilation
                    }
                }
            }
        }

        this.sourceSetToParticipatedCompilations = sourceSetToCompilations
    }

    internal fun initializeTargets(targets: Collection<KotlinTarget>) {
        require(!this::targets.isInitialized) { "Attempt to re-initialize targets for $this. Previous value: ${this.targets}" }
        this.targets = targets
    }

    // overload for small optimization
    override fun isOrphanSourceSet(sourceSet: KotlinSourceSet): Boolean = sourceSet !in sourceSetToParticipatedCompilations.keys

    override fun compilationsBySourceSet(sourceSet: KotlinSourceSet): Collection<KotlinCompilation>? =
        sourceSetToParticipatedCompilations[sourceSet]

    override fun sourceSetByName(name: String): KotlinSourceSet? = sourceSetsByNames[name]
}
