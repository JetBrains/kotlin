/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.mpp

import com.google.common.graph.Graph
import com.intellij.openapi.externalSystem.model.DataNode
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver
import org.jetbrains.kotlin.idea.configuration.KotlinMPPGradleProjectResolver.Companion.CompilationWithDependencies
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil
import org.jetbrains.kotlin.idea.configuration.kotlinAndroidSourceSets
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet
import org.jetbrains.kotlin.idea.configuration.utils.createSourceSetVisibilityGraph
import org.jetbrains.kotlin.idea.configuration.utils.fullName
import org.jetbrains.kotlin.idea.configuration.utils.getKotlinModuleId
import org.jetbrains.kotlin.idea.configuration.utils.transitiveClosure
import org.jetbrains.plugins.gradle.model.ExternalDependency
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData

internal fun KotlinMPPGradleProjectResolver.Companion.populateModuleDependenciesBySourceSetVisibilityGraph(
    context: PopulateModuleDependenciesContext
): Unit = with(context) {
    val sourceSetVisibilityGraph = createSourceSetVisibilityGraph(mppModel).transitiveClosure
    for (sourceSet in sourceSetVisibilityGraph.nodes()) {
        populateSourceSetInfos(context, sourceSetVisibilityGraph, sourceSet)
        if (delegateToAndroidPlugin(sourceSet)) continue


        val visibleSourceSets = sourceSetVisibilityGraph.successors(sourceSet)
        val fromDataNode = getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx)?.cast<GradleSourceSetData>()
            ?: continue


        /* Add dependencies from current sourceSet to all visible source sets (dependsOn, test to production, ...)*/
        for (visibleSourceSet in visibleSourceSets) {
            val toDataNode = getSiblingKotlinModuleData(visibleSourceSet, gradleModule, ideModule, resolverCtx) ?: continue
            addDependency(fromDataNode, toDataNode, visibleSourceSet.isTestModule)
        }

        if (!processedModuleIds.add(getKotlinModuleId(gradleModule, sourceSet, resolverCtx))) continue
        val settings = dependencyPopulationSettings(mppModel, sourceSet)
        val directDependencies = getDependencies(sourceSet).toSet()
        val dependenciesFromVisibleSourceSets = getDependenciesFromVisibleSourceSets(settings, visibleSourceSets)
        val dependenciesFromNativePropagation = getPropagatedNativeDependencies(settings, sourceSet)
        val dependenciesFromPlatformPropagation = getPropagatedPlatformDependencies(sourceSet)

        val dependencies = dependenciesPreprocessor(
            directDependencies + dependenciesFromVisibleSourceSets +
                    dependenciesFromNativePropagation + dependenciesFromPlatformPropagation
        )

        buildDependencies(resolverCtx, sourceSetMap, artifactsMap, fromDataNode, dependencies, ideProject)
    }
}

private data class DependencyPopulationSettings(
    val forceNativeDependencyPropagation: Boolean,
    val excludeInheritedNativeDependencies: Boolean
)

private fun dependencyPopulationSettings(mppModel: KotlinMPPGradleModel, sourceSet: KotlinSourceSet): DependencyPopulationSettings {
    val forceNativeDependencyPropagation: Boolean
    val excludeInheritedNativeDependencies: Boolean
    if (mppModel.extraFeatures.isHMPPEnabled && sourceSet.actualPlatforms.singleOrNull() == KotlinPlatform.NATIVE) {
        forceNativeDependencyPropagation = mppModel.extraFeatures.isNativeDependencyPropagationEnabled
        excludeInheritedNativeDependencies = !forceNativeDependencyPropagation
    } else {
        forceNativeDependencyPropagation = false
        excludeInheritedNativeDependencies = false
    }
    return DependencyPopulationSettings(
        forceNativeDependencyPropagation = forceNativeDependencyPropagation,
        excludeInheritedNativeDependencies = excludeInheritedNativeDependencies
    )
}

private fun PopulateModuleDependenciesContext.getDependenciesFromVisibleSourceSets(
    settings: DependencyPopulationSettings,
    visibleSourceSets: Set<KotlinSourceSet>
): Set<KotlinDependency> {
    val inheritedDependencies = visibleSourceSets
        .flatMap { visibleSourceSet -> getDependencies(visibleSourceSet) }

    return if (settings.excludeInheritedNativeDependencies) {
        inheritedDependencies.filter { !it.name.startsWith(KotlinNativeLibraryNameUtil.KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) }
    } else {
        inheritedDependencies
    }.toSet()
}

private fun PopulateModuleDependenciesContext.getPropagatedNativeDependencies(
    settings: DependencyPopulationSettings,
    sourceSet: KotlinSourceSet
): Set<KotlinDependency> {
    if (!settings.forceNativeDependencyPropagation) {
        return emptySet()
    }

    return getPropagatedNativeDependencies(getCompilationsWithDependencies(sourceSet)).toSet()
}

/**
 * We can't really commonize native platform libraries yet.
 * But APIs for different targets may be very similar.
 * E.g. ios_arm64 and ios_x64 have almost identical platform libraries
 * We handle these special cases and resolve common sources for such
 * targets against libraries of one of them. E.g. common sources for
 * ios_x64 and ios_arm64 will be resolved against ios_arm64 libraries.
 * Currently such special casing is available for Apple platforms
 * (iOS, watchOS and tvOS) and native Android (ARM, X86).
 * TODO: Do we need to support user's interop libraries too?
 */
private fun getPropagatedNativeDependencies(compilations: List<CompilationWithDependencies>): List<ExternalDependency> {
    if (compilations.size <= 1) {
        return emptyList()
    }

    val copyFrom = when {
        compilations.all { it.isAppleCompilation } ->
            compilations.selectFirstAvailableTarget(
                "watchos_arm64", "watchos_arm32", "watchos_x86",
                "ios_arm64", "ios_arm32", "ios_x64",
                "tvos_arm64", "tvos_x64"
            )
        compilations.all { it.konanTarget?.startsWith("android") == true } ->
            compilations.selectFirstAvailableTarget(
                "android_arm64", "android_arm32", "android_x64", "android_x86"
            )
        else -> return emptyList()
    }

    return copyFrom.dependencyNames.mapNotNull { (name, dependency) ->
        when {
            !name.startsWith(KotlinNativeLibraryNameUtil.KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) -> null  // Support only default platform libs for now.
            compilations.all { it.dependencyNames.containsKey(name) } -> dependency
            else -> null
        }
    }
}

private val CompilationWithDependencies.isAppleCompilation: Boolean
    get() = konanTarget?.let {
        it.startsWith("ios") || it.startsWith("watchos") || it.startsWith("tvos")
    } ?: false

private fun Iterable<CompilationWithDependencies>.selectFirstAvailableTarget(
    @NonNls vararg targetsByPriority: String
): CompilationWithDependencies {
    for (target in targetsByPriority) {
        val result = firstOrNull { it.konanTarget == target }
        if (result != null) {
            return result
        }
    }
    return first()
}


private fun PopulateModuleDependenciesContext.getPropagatedPlatformDependencies(
    sourceSet: KotlinSourceSet
): Set<KotlinDependency> {
    if (!mppModel.extraFeatures.isHMPPEnabled) {
        return emptySet()
    }

    return getPropagatedPlatformDependencies(mppModel, sourceSet)
}


/**
 * Source sets sharing code between JVM and Android are the only intermediate source sets that
 * can effectively consume a dependency's platform artifact.
 * When a library only offers a JVM variant, then Android and JVM consume this variant of the library.
 *
 * This will be replaced later on by [KT-43450](https://youtrack.jetbrains.com/issue/KT-43450)
 *
 * @return all dependencies being present across given JVM and Android compilations that this [sourceSet] can also participates in.
 */
private fun getPropagatedPlatformDependencies(
    mppModel: KotlinMPPGradleModel,
    sourceSet: KotlinSourceSet,
): Set<ExternalDependency> {
    if (
        sourceSet.actualPlatforms.platforms.sorted() == listOf(KotlinPlatform.JVM, KotlinPlatform.ANDROID).sorted()
    ) {
        return mppModel.targets
            .filter { target -> target.platform == KotlinPlatform.JVM || target.platform == KotlinPlatform.ANDROID }
            .flatMap { target -> target.compilations }
            .filter { compilation -> compilation.dependsOnSourceSet(mppModel, sourceSet) }
            .map { targetCompilations -> targetCompilations.dependencies.mapNotNull(mppModel.dependencyMap::get).toSet() }
            .reduceOrNull { acc, dependencies -> acc.intersect(dependencies) }.orEmpty()
    }

    return emptySet()
}


// TODO: Move this maybe to another semantic part of KotlinMPPGradleProjectResolver?
private fun KotlinMPPGradleProjectResolver.Companion.populateSourceSetInfos(
    context: PopulateModuleDependenciesContext,
    closedSourceSetGraph: Graph<KotlinSourceSet>,
    sourceSet: KotlinSourceSet
) = with(context) {
    val isAndroid = delegateToAndroidPlugin(sourceSet)
    val fromDataNode = if (isAndroid) ideModule
    else getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx) ?: return

    val dependeeSourceSets = closedSourceSetGraph.successors(sourceSet)
    val sourceSetInfos = if (isAndroid) {
        ideModule.kotlinAndroidSourceSets?.filter {
            (it.kotlinModule as? KotlinCompilation)?.declaredSourceSets?.contains(sourceSet) ?: false
        } ?: emptyList()
    } else {
        listOfNotNull(fromDataNode.kotlinSourceSet)
    }
    for (sourceSetInfo in sourceSetInfos) {
        if (sourceSetInfo.kotlinModule is KotlinCompilation) {
            val selfName = sourceSetInfo.kotlinModule.fullName()
            sourceSetInfo.addSourceSets(dependeeSourceSets, selfName, gradleModule, resolverCtx)
        }
    }
}

inline fun <reified T : Any> DataNode<*>.cast(): DataNode<T> {
    if (data !is T) {
        throw ClassCastException("DataNode<${data.javaClass.canonicalName}> cannot be cast to DataNode<${T::class.java.canonicalName}>")
    }
    @Suppress("UNCHECKED_CAST")
    return this as DataNode<T>
}