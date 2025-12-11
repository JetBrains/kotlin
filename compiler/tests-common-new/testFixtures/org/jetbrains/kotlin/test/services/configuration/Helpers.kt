/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.transitiveDependsOnDependencies
import org.jetbrains.kotlin.test.services.transitiveFriendDependencies
import org.jetbrains.kotlin.test.services.transitiveRegularDependencies
import org.jetbrains.kotlin.utils.addToStdlib.runIf
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

fun createJsTestPhaseConfig(testServices: TestServices, module: TestModule): PhaseConfig {
    val debugMode = DebugMode.fromSystemProperty("kotlin.js.debugMode")
    return if (debugMode >= DebugMode.SUPER_DEBUG) {
        val dumpOutputDir = File(
            JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices),
            testServices.klibEnvironmentConfigurator.getKlibArtifactSimpleName(testServices, module.name) + "-irdump"
        )
        PhaseConfig(
            toDumpStateAfter = PhaseSet.All,
            dumpToDirectory = dumpOutputDir.path,
        )
    } else {
        PhaseConfig()
    }
}

fun CompilerConfiguration.addSourcesForDependsOnClosure(
    module: TestModule,
    testServices: TestServices,
) {
    val isMppCompilation = module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
    for (mppModule in module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)) {
        for (file in mppModule.kotlinFiles) {
            addKotlinSourceRoot(
                path = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(file).canonicalPath,
                hmppModuleName = runIf(isMppCompilation) { mppModule.name }
            )
        }
    }
}
