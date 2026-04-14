/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.services

import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ReplSnippetCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.updateClasspath

/**
 * Compiles REPL snippets to JARs using [K2ReplCompiler].
 *
 * Must be called sequentially for each snippet in order (snippet_001, snippet_002, ...).
 * The compiler maintains REPL history across calls, so each snippet can reference
 * declarations from all previous snippets.
 */
class ReplSnippetCompilationService(private val testServices: TestServices) : ReplSnippetCompiler {
    private var replCompiler: K2ReplCompiler? = null

    override fun compileSnippetToJar(testModule: TestModule, testServices: TestServices): List<Path> {
        val compiler = getOrCreateCompiler()

        val testFile = testModule.files.single()
        val fileText = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
        val snippetSource = fileText.toScriptSource(testFile.name)

        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compiler.compile(snippetSource)
        }

        val compiledSnippet = result.valueOrThrow()
        val compiledScript = compiledSnippet.get() as KJvmCompiledScript
        val compiledModule = compiledScript.getCompiledModule() as KJvmCompiledModuleInMemory

        val outputDir = testServices.getOrCreateTempDirectory("replSnippets")
        val outputJar = outputDir.resolve("${testModule.name}.jar").toPath()

        JarOutputStream(FileOutputStream(outputJar.toFile())).use { jar ->
            for ((path, bytes) in compiledModule.compilerOutputFiles) {
                jar.putNextEntry(JarEntry(path))
                jar.write(bytes)
                jar.closeEntry()
            }
        }

        return listOf(outputJar)
    }

    private fun getOrCreateCompiler(): K2ReplCompiler {
        return replCompiler ?: run {
            val messageCollector = ScriptDiagnosticsMessageCollector(null)
            val emptyPluginDir = testServices.getOrCreateTempDirectory("emptyPluginDir").absolutePath
            val config = ScriptCompilationConfiguration {
                updateClasspath(listOf(testServices.standardLibrariesPathProvider.runtimeJarForTests()))
                compilerOptions("-Xplugin=$emptyPluginDir")
            }
            K2ReplCompiler(
                K2ReplCompiler.createCompilationState(
                    messageCollector,
                    testServices.compilerConfigurationProvider.testRootDisposable,
                    config,
                )
            )
        }.also { replCompiler = it }
    }
}
