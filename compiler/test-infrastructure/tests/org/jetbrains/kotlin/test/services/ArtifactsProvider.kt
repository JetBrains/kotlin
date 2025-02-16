/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule

/**
 * This service stores artifacts produced by all test facades for each module
 */
class ArtifactsProvider(
    private val testServices: TestServices,
    private val testModules: List<TestModule>
) : TestService {
    private val artifactsByModule: MutableMap<TestModule, MutableMap<TestArtifactKind<*>, ResultingArtifact<*>>> = mutableMapOf()

    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> getArtifactSafe(
        module: TestModule,
        kind: TestArtifactKind<OutputArtifact>,
    ): OutputArtifact? {
        @Suppress("UNCHECKED_CAST")
        return artifactsByModule.getMap(module)[kind] as OutputArtifact?
    }

    fun <A : ResultingArtifact<A>> getArtifact(module: TestModule, kind: TestArtifactKind<A>): A {
        return getArtifactSafe(module, kind) ?: error("Artifact with kind $kind is not registered for module ${module.name}")
    }

    // Registers output artifact, overriding previous one of the same kind.
    // It's important for different IrBackend artifacts: the first one before serialization, the second one after serialization.
    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> registerArtifact(
        module: TestModule,
        artifact: ResultingArtifact<OutputArtifact>,
    ) {
        artifactsByModule.getMap(module)[artifact.kind] = artifact
    }

    fun unregisterAllArtifacts(module: TestModule) {
        artifactsByModule.remove(module)
    }

    fun copy(): ArtifactsProvider {
        return ArtifactsProvider(testServices, testModules).also {
            artifactsByModule.putAll(artifactsByModule.mapValues { (_, map) -> map.toMutableMap() })
        }
    }

    private fun <K, V, R> MutableMap<K, MutableMap<V, R>>.getMap(key: K): MutableMap<V, R> {
        return getOrPut(key) { mutableMapOf() }
    }
}

val TestServices.artifactsProvider: ArtifactsProvider by TestServices.testServiceAccessor()
