/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import java.io.File

fun getKlibDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<File> {
    val visited = mutableSetOf<TestModule>()
    fun getRecursive(module: TestModule, relation: DependencyRelation) {
        val dependencies = if (relation == DependencyRelation.FriendDependency) {
            module.friendDependencies
        } else {
            module.regularDependencies
        }
        dependencies
            // See: `dependencyKind =` in AbstractJsBlackBoxCodegenTestBase.kt
            .filter { it.kind != DependencyKind.Source }
            .map { testServices.dependencyProvider.getTestModule(it.moduleName) }.forEach {
                if (it !in visited) {
                    visited += it
                    getRecursive(it, relation)
                }
            }
    }
    getRecursive(module, kind)
    return visited.map { testServices.dependencyProvider.getArtifact(it, ArtifactKinds.KLib).outputFile }
}

fun getDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<ModuleDescriptor> {
    return getKlibDependencies(module, testServices, kind)
        .map { testServices.libraryProvider.getDescriptorByPath(it.absolutePath) }
}
