/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.projectStructure

import com.intellij.openapi.vfs.StandardFileSystems
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
import java.nio.file.Paths

class StandaloneLibraryModuleScopeTest : AbstractStandaloneTest() {
    override val suiteName: Path
        get() = Paths.get("projectStructure", "libraryModuleScope")

    @Test
    fun testLibraryModuleScopeUsesParentTraversalByDefault() {
        val libraryJar = compileToJar(testDataPath("library"))
        val otherLibraryJar = compileToJar(testDataPath("otherLibrary"))

        lateinit var libraryModule: KaLibraryModule

        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                libraryModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(libraryJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "library"
                    }
                )
                addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(otherLibraryJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "otherLibrary"
                    }
                )
            }
        }

        val files = collectLibraryFiles(libraryJar, otherLibraryJar, session)

        assertLibraryScope(libraryModule, "Parent-traversal library search scope", files)
    }

    @Test
    @OptIn(StandaloneWorkaroundApi::class)
    fun testLibraryModuleScopeRespectsProviderDefaultAndModuleOverride() {
        val libraryJar = compileToJar(testDataPath("library"))
        val otherLibraryJar = compileToJar(testDataPath("otherLibrary"))

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
                        addBinaryRoot(libraryJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "inherited"
                    }
                )

                // The following modules each override the provider-wide default with a specific mode.
                parentTraversalModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(libraryJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.ParentTraversal
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "parentTraversalOverride"
                    }
                )
                trieModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(libraryJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.Trie
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "trieOverride"
                    }
                )
                enumerationModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(libraryJar)
                        libraryScopeConstructionMode = StandaloneLibraryScopeConstructionMode.Enumeration
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "enumerationOverride"
                    }
                )

                addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(otherLibraryJar)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "otherLibrary"
                    }
                )
            }
        }

        val files = collectLibraryFiles(libraryJar, otherLibraryJar, session)

        assertLibraryScope(inheritedModule, "Trie-based library search scope", files)
        assertLibraryScope(parentTraversalModule, "Parent-traversal library search scope", files)
        assertLibraryScope(trieModule, "Trie-based library search scope", files)
        assertLibraryScope(enumerationModule, "Enumeration-based library search scope", files)
    }

    /**
     * @property containedFiles The files which the library scope should contain.
     * @property nonContainedFiles The files which the library scope should *not* contain.
     */
    private class LibraryFiles(
        val containedFiles: Collection<VirtualFile>,
        val nonContainedFiles: Collection<VirtualFile>,
    )

    private fun collectLibraryFiles(libraryJar: Path, otherLibraryJar: Path, session: StandaloneAnalysisAPISession): LibraryFiles {
        val libraryJarRoot = getJarRootVirtualFile(libraryJar, session)
        val otherLibraryJarRoot = getJarRootVirtualFile(otherLibraryJar, session)

        val containedFiles = LibraryUtils.getAllVirtualFilesFromRoot(libraryJarRoot, includeRoot = true)

        val nonContainedFiles = buildList {
            // All files of a different JAR, including its root. Both JARs are called `library.jar`, but reside in different directories.
            addAll(LibraryUtils.getAllVirtualFilesFromRoot(otherLibraryJarRoot, includeRoot = true))

            // The JAR archive itself and the directory containing it, both in the local file system.
            add(getLocalVirtualFile(libraryJar))
            add(getLocalVirtualFile(libraryJar.parent))

            // A source file from which the library was compiled.
            add(getLocalVirtualFile(testDataPath("library").resolve("library.kt")))
        }

        Assertions.assertTrue(containedFiles.any { !it.isDirectory }, "The library JAR should contain at least one file.")
        Assertions.assertTrue(
            nonContainedFiles.any { !it.isDirectory && it.url.startsWith(otherLibraryJarRoot.url) },
            "The other library JAR should contain at least one file.",
        )

        return LibraryFiles(containedFiles, nonContainedFiles)
    }

    private fun getJarRootVirtualFile(jar: Path, session: StandaloneAnalysisAPISession): VirtualFile =
        StandaloneProjectFactory.getVirtualFilesForLibraryRoots(listOf(jar), session.coreApplicationEnvironment).single()

    private fun getLocalVirtualFile(path: Path): VirtualFile =
        StandardFileSystems.local().findFileByPath(path.toAbsolutePath().toString())
            ?: error("Cannot find a local virtual file for `$path`.")

    private fun assertLibraryScope(
        module: KaLibraryModule,
        expectedDescriptionPrefix: String,
        files: LibraryFiles,
    ) {
        val scope = module.baseContentScope
        Assertions.assertTrue(
            scope.toString().startsWith(expectedDescriptionPrefix),
            "Expected a library scope matching \"$expectedDescriptionPrefix\", but got: $scope",
        )

        for (file in files.containedFiles) {
            Assertions.assertTrue(scope.contains(file), "The scope of `${module.libraryName}` should contain `$file`.")
        }

        for (file in files.nonContainedFiles) {
            Assertions.assertFalse(scope.contains(file), "The scope of `${module.libraryName}` should not contain `$file`.")
        }
    }
}
