/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.test.services.transitiveFriendDependencies
import org.jetbrains.kotlin.test.services.transitiveRegularDependencies
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

fun getKlibDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<File> {
    val filter: (DependencyDescription) -> Boolean = { it.kind != DependencyKind.Source }
    val dependencies = when (kind) {
        DependencyRelation.RegularDependency -> module.transitiveRegularDependencies(filter = filter)
        DependencyRelation.FriendDependency -> module.transitiveFriendDependencies(filter = filter)
        DependencyRelation.DependsOnDependency -> shouldNotBeCalled()
    }
    return dependencies.map { testServices.artifactsProvider.getArtifact(it, ArtifactKinds.KLib).outputFile }
}

fun getDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<ModuleDescriptor> {
    return getKlibDependencies(module, testServices, kind)
        .map { testServices.libraryProvider.getDescriptorByPath(it.absolutePath) }
}

fun getFriendDependencies(module: TestModule, testServices: TestServices): Set<ModuleDescriptorImpl> =
    getDependencies(module, testServices, DependencyRelation.FriendDependency)
        .filterIsInstanceTo<ModuleDescriptorImpl, MutableSet<ModuleDescriptorImpl>>(mutableSetOf())
