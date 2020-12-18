/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.GradleImportPropeties.*
import kotlin.reflect.KProperty

internal class MultiplatformModelImportingContext(
    val project: Project,
    val targets: Collection<KotlinTarget>,
    val sourceSetsToCompilations: Map<KotlinSourceSet, Collection<KotlinCompilation>>,
    val sourceSetsByNames: Map<String, KotlinSourceSetImpl>,
    val properties: ImportProperties,
) {
    /**
     * All source sets in a project, including those that are created but not included into any compilations
     * (so-called "orphan" source sets). Use [isOrphanSourceSet] to get only compiled source sets
     */
    val sourceSets: Collection<KotlinSourceSetImpl> = sourceSetsByNames.values

    val compilations: Collection<KotlinCompilation> = sourceSetsToCompilations.values.flatten()

    fun sourceSetByName(name: String): KotlinSourceSet? = sourceSetsByNames[name]

    fun compilationsBySourceSet(sourceSet: KotlinSourceSet): Collection<KotlinCompilation>? = sourceSetsToCompilations[sourceSet]

    /**
     * "Orphan" is a source set which is not actually compiled by the compiler, i.e. the one
     * which doesn't belong to any [KotlinCompilation].
     *
     * Orphan source sets might appear if one creates a source-set manually and doesn't link
     * it anywhere (essentially this is a misconfiguration)
     */
    fun isOrphanSourceSet(sourceSet: KotlinSourceSet): Boolean = compilationsBySourceSet(sourceSet) == null
}

class ImportProperties(val project: Project) {
    val isHmppEnabled: Boolean by IS_HMPP_ENABLED
    val coerceRootSourceSetsToCommon: Boolean by COERCE_ROOT_SOURCE_SETS_TO_COMMON
    val enableNativeDependencyPropagation: Boolean by ENABLE_NATIVE_DEPENDENCY_PROPAGATION
    val buildMetadataDependencies: Boolean by BUILD_METADATA_DEPENDENCIES
    val importOrphanSourceSets: Boolean by IMPORT_ORPHAN_SOURCE_SETS
    val includeAndroidDependencies: Boolean by INCLUDE_ANDROID_DEPENDENCIES
}

private enum class GradleImportPropeties(val id: String, val defaultValue: Boolean) {
    IS_HMPP_ENABLED("kotlin.mpp.enableGranularSourceSetsMetadata", false),
    COERCE_ROOT_SOURCE_SETS_TO_COMMON("kotlin.mpp.coerceRootSourceSetsToCommon", true),
    ENABLE_NATIVE_DEPENDENCY_PROPAGATION("kotlin.native.enableDependencyPropagation", true),
    BUILD_METADATA_DEPENDENCIES("build_metadata_dependencies_for_actualised_source_sets", true),
    IMPORT_ORPHAN_SOURCE_SETS("import_orphan_source_sets", true),
    INCLUDE_ANDROID_DEPENDENCIES("kotlin.include.android.dependencies", false)
    ;

    operator fun getValue(thisRef: ImportProperties, property: KProperty<*>): Boolean =
        (thisRef.project.findProperty(this.id) as? String)?.toBoolean() ?: this.defaultValue
}


class MultiplatformModelImportingContextBuilder(val project: Project) {

    lateinit var targets: Collection<KotlinTarget>

    val compilations: Collection<KotlinCompilation> by lazy { targets.flatMap { it.compilations } }

    lateinit var sourceSetsByNames: Map<String, KotlinSourceSetImpl>
    val sourceSets: Collection<KotlinSourceSetImpl>
        get() = sourceSetsByNames.values

    val properties: ImportProperties = ImportProperties(project)

    // overload for small optimization
    fun isOrphanSourceSet(sourceSet: KotlinSourceSet): Boolean = sourceSet in sourceSetToParticipatedCompilations.keys

    fun compilationsBySourceSet(sourceSet: KotlinSourceSet): Collection<KotlinCompilation>? =
        sourceSetToParticipatedCompilations[sourceSet]

    fun sourceSetByName(name: String): KotlinSourceSet? = sourceSetsByNames[name]

    private val sourceSetToParticipatedCompilations: Map<KotlinSourceSet, Set<KotlinCompilation>> by lazy {
        // includes compilations where source set is included via dependsOn
        val allSourceSetToCompilations = LinkedHashMap<KotlinSourceSet, MutableSet<KotlinCompilation>>()

        for (target in targets) {
            for (compilation in target.compilations) {
                for (sourceSet in compilation.sourceSets) {
                    allSourceSetToCompilations.getOrPut(sourceSet) { LinkedHashSet() } += compilation
                    sourceSet.dependsOnSourceSets.mapNotNull { sourceSetByName(it) }.forEach {
                        allSourceSetToCompilations.getOrPut(it) { LinkedHashSet() } += compilation
                    }
                }
            }
        }

        allSourceSetToCompilations
    }

    internal fun build(): MultiplatformModelImportingContext =
        MultiplatformModelImportingContext(project, targets, sourceSetToParticipatedCompilations, sourceSetsByNames, properties)
}
