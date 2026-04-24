/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.trackers.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.initializeBtaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ImportTrackerTest : BaseCompilationTest() {
    @DisplayName("Check internal import tracker output in non-incremental mode in-process")
    @Test
    fun importsNonIncremental() {
        val moduleName = "jvm-module-3"
        // also check order and quantity
        val expectedFileToImport =
            listOf(
                "/src/bar.kt" to "p.foo",
                "/src/foo.kt" to "p2.*",
            )
        val kotlinToolchains = KotlinToolchains.loadImplementation(customBtaClassLoader)
        val inProcessPolicy = kotlinToolchains.createInProcessExecutionPolicy()
        jvmProject(kotlinToolchains to inProcessPolicy) {
            val module1 = module(moduleName)
            val recordedImports = mutableListOf<Pair<String, String>>()
            val importTracker = object : CompilerImportTracker {
                override fun report(filePath: String, importedFqName: String) {
                    val relativePath = filePath.substringAfter(moduleName)
                    recordedImports.add(relativePath to importedFqName)
                }
            }
            module1.compile(compilationConfigAction = { builder: JvmCompilationOperation.Builder ->
                @Suppress("DEPRECATION_ERROR")
                builder[BuildOperation.createCustomOption("IMPORT_TRACKER")] = importTracker
            }) {
                assertEquals(expectedFileToImport, recordedImports) { "Import tracker didn't produce expected output" }
            }
        }
    }

    companion object {
        private val customBtaClassLoader = initializeBtaClassloader(ImportTrackerTestClassLoader())
    }

    private class ImportTrackerTestClassLoader(
    ) : ClassLoader() {
        private val thisClassloader: ClassLoader = ImportTrackerTestClassLoader::class.java.classLoader
        private val sharedApiClassesClassLoader: ClassLoader = SharedApiClassesClassLoader()

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            return if (name == CompilerImportTracker::class.java.name) {
                thisClassloader.loadClass(name)
            } else {
                sharedApiClassesClassLoader.loadClass(name)
            }
        }
    }
}
