/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.HAS_CUSTOM_EXTENSION_FILES
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
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
        val files = if (HAS_CUSTOM_EXTENSION_FILES in module.directives) mppModule.files else mppModule.kotlinFiles
        for (file in files) {
            addKotlinSourceRoot(
                path = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(file).canonicalPath,
                hmppModuleName = runIf(isMppCompilation) { mppModule.name }
            )
        }
    }
}

/**
 * Returns the FQ-name of the package of the file that contains the `box` function.
 */
fun extractTestPackage(testServices: TestServices, ignoreEsModules: Boolean = true): FqName {
    val runPlainBoxFunction = JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION in testServices.moduleStructure.allDirectives
    if (runPlainBoxFunction) return FqName.ROOT

    val ktFiles = testServices.moduleStructure.modules.flatMap { module ->
        module.files
            .filter { it.isKtFile }
            .map {
                val project = testServices.compilerConfigurationProvider.getProject(module)
                module to testServices.sourceFileProvider.getKtFileForSourceFile(it, project)
            }
    }

    val fileWithBoxFunction = ktFiles.find { (module, ktFile) ->
        (!ignoreEsModules || JsEnvironmentConfigurator.getModuleKind(testServices, module) != ModuleKind.ES) &&
                ktFile.declarations.find { it is KtNamedFunction && it.name == "box" } != null
    } ?: return FqName.ROOT

    return fileWithBoxFunction.second.packageFqName
}

fun String.finalizePath(moduleKind: ModuleKind): String {
    return plus(moduleKind.jsExtension).minifyPathForWindowsIfNeeded()
}

/**
 * D8 ignores Windows settings related to extending of maximum path symbols count
 * The hack should be deleted when D8 fixes the bug.
 * The issue is here: https://bugs.chromium.org/p/v8/issues/detail?id=13318
*/
fun String.minifyPathForWindowsIfNeeded(): String {
    if (!isWindows) return this
    val delimiter = if (contains('\\')) '\\' else '/'
    val directoryPath = substringBeforeLast(delimiter)
    val fileFullName = substringAfterLast(delimiter)
    val fileName = fileFullName.substringBeforeLast('.')

    if (fileName.length <= 80) return this

    val fileExtension = fileFullName.substringAfterLast('.')
    val extensionPart = if (fileExtension.isEmpty()) "" else ".$fileExtension"

    return "$directoryPath$delimiter${fileName.cityHash64().toULong().toString(16)}$extensionPart"
}
