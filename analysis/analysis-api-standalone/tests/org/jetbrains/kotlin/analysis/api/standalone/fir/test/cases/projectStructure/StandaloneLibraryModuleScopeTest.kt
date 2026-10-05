/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.projectStructure

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AbstractStandaloneTest
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder.compileToJar
import org.jetbrains.kotlin.analysis.api.standalone.projectStructure.StandaloneLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class StandaloneLibraryModuleScopeTest : AbstractStandaloneTest() {
    override val suiteName: String
        get() = "projectStructure"

    @Test
    fun testLibraryModuleScopeUsesParentTraversalByDefault() {
        val compiledJar = compileToJar(testDataPath(ROOT).resolve("library"))

        lateinit var libraryModule: KaLibraryModule

        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                libraryModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(compiledJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "dependency"
                    }
                )
            }
        }

        val jarRoot = getJarRootVirtualFile(compiledJar, session)
        val fileInJar = findFirstFileInJar(jarRoot)

        assertLibraryScopeKindAndContainment(libraryModule, "Parent-traversal library search scope", jarRoot, fileInJar)
    }

    @Test
    @OptIn(StandaloneWorkaroundApi::class)
    fun testLibraryModuleScopeRespectsProviderDefaultAndModuleOverride() {
        val compiledJar = compileToJar(testDataPath(ROOT).resolve("library"))

        lateinit var inheritedModule: KaLibraryModule
        lateinit var parentTraversalModule: KaLibraryModule
        lateinit var trieModule: KaLibraryModule
        lateinit var enumerationModule: KaLibraryModule

        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform

                // A provider-wide default that differs from the global default ([StandaloneLibraryScopeConstructionMode.ParentTraversal]).
                libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.Trie

                // Inherits the provider-wide default.
                inheritedModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(compiledJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "inherited"
                    }
                )

                // The following modules each override the provider-wide default with a specific mode.
                parentTraversalModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(compiledJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.ParentTraversal
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "parentTraversalOverride"
                    }
                )
                trieModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(compiledJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.Trie
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "trieOverride"
                    }
                )
                enumerationModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(compiledJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.Enumeration
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "enumerationOverride"
                    }
                )
            }
        }

        val jarRoot = getJarRootVirtualFile(compiledJar, session)
        val fileInJar = findFirstFileInJar(jarRoot)

        assertLibraryScopeKindAndContainment(inheritedModule, "Trie-based library search scope", jarRoot, fileInJar)
        assertLibraryScopeKindAndContainment(parentTraversalModule, "Parent-traversal library search scope", jarRoot, fileInJar)
        assertLibraryScopeKindAndContainment(trieModule, "Trie-based library search scope", jarRoot, fileInJar)
        assertLibraryScopeKindAndContainment(enumerationModule, "Enumeration-based library search scope", jarRoot, fileInJar)
    }

    private fun getJarRootVirtualFile(jar: Path, session: StandaloneAnalysisAPISession): VirtualFile =
        StandaloneProjectFactory.getVirtualFilesForLibraryRoots(listOf(jar), session.coreApplicationEnvironment).single()

    private fun findFirstFileInJar(jarRoot: VirtualFile): VirtualFile =
        LibraryUtils.getAllVirtualFilesFromRoot(jarRoot, includeRoot = false).first()

    private fun assertLibraryScopeKindAndContainment(
        module: KaLibraryModule,
        expectedDescriptionPrefix: String,
        jarRoot: VirtualFile,
        fileInJar: VirtualFile,
    ) {
        val scope = module.baseContentScope
        Assertions.assertTrue(
            scope.toString().startsWith(expectedDescriptionPrefix),
            "Expected a library scope matching \"$expectedDescriptionPrefix\", but got: $scope",
        )
        Assertions.assertTrue(scope.contains(jarRoot), "The scope should contain the JAR root: $jarRoot")
        Assertions.assertTrue(scope.contains(fileInJar), "The scope should contain a file from the JAR: $fileInJar")
    }

    private companion object {
        const val ROOT = "libraryModuleScope"
    }
}
