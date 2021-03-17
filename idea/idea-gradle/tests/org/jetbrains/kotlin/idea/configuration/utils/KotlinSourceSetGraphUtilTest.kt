/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.utils

import com.google.common.graph.EndpointPair
import com.google.common.graph.EndpointPair.ordered
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.junit.Test
import kotlin.test.assertEquals

class KotlinSourceSetGraphUtilTest {

    @Test
    fun `simple layout`() {
        val sourceSetsByName = mapOf(
            createKotlinSourceSetPair(COMMON_MAIN_SOURCE_SET_NAME),
            createKotlinSourceSetPair(COMMON_TEST_SOURCE_SET_NAME),
            createKotlinSourceSetPair("jvmMain", setOf(COMMON_MAIN_SOURCE_SET_NAME)),
            createKotlinSourceSetPair("jvmTest", setOf(COMMON_TEST_SOURCE_SET_NAME))
        )
        val dependsOnGraph = createSourceSetDependsOnGraph(sourceSetsByName)

        assertEquals(
            listOf(
                ordered("jvmMain", COMMON_MAIN_SOURCE_SET_NAME),
                ordered("jvmTest", COMMON_TEST_SOURCE_SET_NAME)
            ).sorted(),
            dependsOnGraph.nameEdges().sorted()
        )
    }

    @Test
    fun `hierarchical layout`() {
        val sourceSetsByName = mapOf(
            createKotlinSourceSetPair(COMMON_MAIN_SOURCE_SET_NAME),
            createKotlinSourceSetPair(COMMON_TEST_SOURCE_SET_NAME),

            createKotlinSourceSetPair("jvmAndJsMain", setOf(COMMON_MAIN_SOURCE_SET_NAME)),
            createKotlinSourceSetPair("jvmAndJsTest", setOf(COMMON_TEST_SOURCE_SET_NAME)),

            createKotlinSourceSetPair("jvmMain", setOf("jvmAndJsMain")),
            createKotlinSourceSetPair("jvmTest", setOf("jvmAndJsTest")),

            createKotlinSourceSetPair("jsMain", setOf("jvmAndJsMain")),
            createKotlinSourceSetPair("jsTest", setOf("jvmAndJsTest")),
        )

        val dependsOnGraph = createSourceSetDependsOnGraph(sourceSetsByName)

        assertEquals(
            listOf(
                ordered("jvmMain", "jvmAndJsMain"),
                ordered("jvmTest", "jvmAndJsTest"),
                ordered("jsMain", "jvmAndJsMain"),
                ordered("jsTest", "jvmAndJsTest"),
                ordered("jvmAndJsMain", COMMON_MAIN_SOURCE_SET_NAME),
                ordered("jvmAndJsTest", COMMON_TEST_SOURCE_SET_NAME)
            ).sorted(),
            dependsOnGraph.nameEdges().sorted()
        )
    }

    @Test
    fun `source set depending on multiple other source sets`() {
        val sourceSetsByName = mapOf(
            createKotlinSourceSetPair("common1"),
            createKotlinSourceSetPair("common2"),
            createKotlinSourceSetPair("consumerA", setOf("common1", "common2")),
            createKotlinSourceSetPair("consumerB", setOf("common1", "consumerA"))
        )

        val dependsOnGraph = createSourceSetDependsOnGraph(sourceSetsByName)


        assertEquals(
            listOf(
                ordered("consumerA", "common1"),
                ordered("consumerA", "common2"),
                ordered("consumerB", "common1"),
                ordered("consumerB", "consumerA")
            ).sorted(),
            dependsOnGraph.nameEdges().sorted()
        )
    }

    @Test
    fun `infer default test to production edges`() {
        val sourceSetsByName = mapOf(
            createKotlinSourceSetPair(COMMON_MAIN_SOURCE_SET_NAME),
            createKotlinSourceSetPair(COMMON_TEST_SOURCE_SET_NAME, isTestModule = true),
            createKotlinSourceSetPair("main"),
            createKotlinSourceSetPair("test", isTestModule = true),
            createKotlinSourceSetPair("abcMain"),
            createKotlinSourceSetPair("abcTest", isTestModule = true)
        )

        val graph = GraphBuilder.directed().build<KotlinSourceSet>()
        sourceSetsByName.values.forEach { sourceSet -> graph.addNode(sourceSet) }
        graph.putInferredTestToProductionEdges()

        assertEquals(
            listOf(
                ordered(COMMON_TEST_SOURCE_SET_NAME, COMMON_MAIN_SOURCE_SET_NAME),
                ordered("test", "main"),
                ordered("abcTest", "abcMain")
            ).sorted(),
            graph.nameEdges().sorted()
        )
    }

    @Test
    fun `depends on graph will automatically infer missing dependsOn edges for Android source sets`() {
        val sourceSetsByName = mapOf(
            createKotlinSourceSetPair(COMMON_MAIN_SOURCE_SET_NAME),
            createKotlinSourceSetPair(COMMON_TEST_SOURCE_SET_NAME, isTestModule = true),
            createKotlinSourceSetPair("androidMain", platforms = setOf(KotlinPlatform.ANDROID)),
            createKotlinSourceSetPair("androidTest", platforms = setOf(KotlinPlatform.ANDROID), isTestModule = true)
        )

        assertEquals(
            listOf(
                ordered("androidMain", COMMON_MAIN_SOURCE_SET_NAME),
                ordered("androidTest", COMMON_TEST_SOURCE_SET_NAME)
            ).sorted(),
            createSourceSetDependsOnGraph(sourceSetsByName).nameEdges().sorted()
        )
    }
}

private fun createKotlinSourceSetPair(
    name: String,
    declaredDependsOnSourceSets: Set<String> = emptySet(),
    allDependsOnSourceSets: Set<String> = declaredDependsOnSourceSets,
    platforms: Set<KotlinPlatform> = emptySet(),
    isTestModule: Boolean = false,
): Pair<String, KotlinSourceSet> = name to KotlinSourceSetImpl(
    name = name,
    languageSettings = KotlinLanguageSettingsImpl(
        languageVersion = null,
        apiVersion = null,
        isProgressiveMode = false,
        enabledLanguageFeatures = emptySet(),
        experimentalAnnotationsInUse = emptySet(),
        compilerPluginArguments = emptyArray(),
        compilerPluginClasspath = emptySet(),
        freeCompilerArgs = emptyArray()
    ),
    sourceDirs = emptySet(),
    resourceDirs = emptySet(),
    dependencies = emptyArray(),
    declaredDependsOnSourceSets = declaredDependsOnSourceSets,
    allDependsOnSourceSets = allDependsOnSourceSets,
    defaultActualPlatforms = KotlinPlatformContainerImpl().apply { pushPlatforms(platforms) },
    defaultIsTestModule = isTestModule
)

private fun Graph<KotlinSourceSet>.nameEdges() = edges()
    .map { edge -> ordered(edge.source().name, edge.target().name) }

private fun Iterable<EndpointPair<String>>.sorted() = this.sortedBy { it.source() + " -> " + it.target() }