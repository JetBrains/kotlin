/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.configuration.utils

import com.google.common.graph.*
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.gradle.KotlinSourceSet

internal fun createSourceSetVisibilityGraph(model: KotlinMPPGradleModel): ImmutableGraph<KotlinSourceSet> {
    val graph = createSourceSetDependsOnGraph(model)
    graph.putInferredTestToProductionEdges()
    return graph.immutable
}

internal fun createSourceSetDependsOnGraph(model: KotlinMPPGradleModel): MutableGraph<KotlinSourceSet> {
    return createSourceSetDependsOnGraph(model.sourceSetsByName)
}

internal fun createSourceSetDependsOnGraph(
    sourceSetsByName: Map<String, KotlinSourceSet>
): MutableGraph<KotlinSourceSet> {
    val graph = GraphBuilder.directed().build<KotlinSourceSet>()
    val sourceSets = sourceSetsByName.values.toSet()

    for (sourceSet in sourceSets) {
        graph.addNode(sourceSet)
        val dependsOnSourceSets = getFixedDependsOnSourceSets(sourceSetsByName, sourceSet)
        for (dependsOnSourceSet in dependsOnSourceSets) {
            graph.addNode(dependsOnSourceSet)
            graph.putEdge(sourceSet, dependsOnSourceSet)
        }
    }

    return graph
}

internal fun MutableGraph<KotlinSourceSet>.putInferredTestToProductionEdges() {
    val sourceSets = this.nodes()
    for (sourceSet in sourceSets) {
        if (sourceSet.isTestModule) {
            @OptIn(UnsafeTestSourceSetHeuristicApi::class)
            val predictedMainSourceSetName = predictedProductionSourceSetName(sourceSet.name)
            val predictedMainSourceSet = sourceSets.firstOrNull { it.name == predictedMainSourceSetName } ?: continue
            putEdge(sourceSet, predictedMainSourceSet)
        }
    }
}

private fun getFixedDependsOnSourceSets(
    sourceSetsByName: Map<String, KotlinSourceSet>, sourceSet: KotlinSourceSet
): Set<KotlinSourceSet> {
    /*
    Workaround for older Kotlin Gradle Plugin versions that did not explicitly declare a dependsOn relation
    from a Kotlin source set to "commonMain"
    (Can probably be dropped in Kotlin 1.5)
     */
    val implicitDependsOnEdgeForAndroid = if (
        sourceSet.actualPlatforms.contains(KotlinPlatform.ANDROID) && sourceSet.declaredDependsOnSourceSets.isEmpty()
    ) {
        val commonSourceSetName = if (sourceSet.isTestModule) KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
        else KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        listOfNotNull(sourceSetsByName[commonSourceSetName])
    } else emptyList()

    return sourceSet.declaredDependsOnSourceSets.map(sourceSetsByName::getValue)
        .plus(implicitDependsOnEdgeForAndroid)
        /*
        Gracefully filter out source sets that declare a dependency on themselves.
        This also fixes KTIJ-1025
        */
        .filter { dependencySourceSet -> dependencySourceSet != sourceSet }
        .toSet()
}

/**
 * @see Graphs.transitiveClosure
 */
internal val <T> Graph<T>.transitiveClosure: Graph<T> get() = Graphs.transitiveClosure(this)

/**
 * @see ImmutableGraph.copyOf
 */
internal val <T> Graph<T>.immutable: ImmutableGraph<T> get() = ImmutableGraph.copyOf(this)