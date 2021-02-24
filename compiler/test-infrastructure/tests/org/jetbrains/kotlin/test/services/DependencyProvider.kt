/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.*

abstract class DependencyProvider : TestService {
    abstract fun getTestModule(name: String): TestModule

    abstract fun <A : ResultingArtifact<A>> getArtifact(module: TestModule, kind: TestArtifactKind<A>): A
}

val TestServices.dependencyProvider: DependencyProvider by TestServices.testServiceAccessor()

class DependencyProviderImpl(
    private val testServices: TestServices,
    testModules: List<TestModule>
) : DependencyProvider() {
    private val assertions: Assertions
        get() = testServices.assertions

    private val testModulesByName = testModules.map { it.name to it }.toMap()

    private val artifactsByModule: MutableMap<TestModule, MutableMap<TestArtifactKind<*>, ResultingArtifact<*>>> = mutableMapOf()

    override fun getTestModule(name: String): TestModule {
        return testModulesByName[name] ?: assertions.fail { "Module $name is not defined" }
    }

    override fun <A : ResultingArtifact<A>> getArtifact(module: TestModule, kind: TestArtifactKind<A>): A {
        val artifact = artifactsByModule.getMap(module)[kind]
            ?: error("Artifact with kind $kind is not registered for module ${module.name}")
        @Suppress("UNCHECKED_CAST")
        return artifact as A
    }

    fun <A : ResultingArtifact<A>> registerArtifact(module: TestModule, artifact: ResultingArtifact<A>) {
        val kind = artifact.kind
        val previousValue = artifactsByModule.getMap(module).put(kind, artifact)
        if (previousValue != null) error("Artifact with kind $kind already registered for module ${module.name}")
    }

    private fun <K, V, R> MutableMap<K, MutableMap<V, R>>.getMap(key: K): MutableMap<V, R> {
        return getOrPut(key) { mutableMapOf() }
    }
}
