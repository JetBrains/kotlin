/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.util.ServiceLoaderLite
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader

/** Private request/response protocol used by the matching Build Tools API implementation. */
internal object NativeImageCompilerServer {
    fun run() {
        val input = DataInputStream(System.`in`.buffered())
        val output = DataOutputStream(System.out.buffered())
        val originalOut = System.out
        val originalErr = System.err
        // Reserve stdout for the protocol, including during compiler environment initialization.
        System.setOut(originalErr)
        output.writeInt(0x4b4e4931)
        output.flush()
        val disposable = Disposer.newDisposable("native compiler build session")
        try {
            setupIdeaStandaloneExecution()
            @OptIn(CoreEnvironmentDeprecation::class)
            KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(disposable, CompilerConfiguration.create())
            CachedPluginsLoader().use { plugins ->
                val services = Services.Builder().register(PluginsLoader::class.java, plugins).build()
                while (true) {
                    val count = try {
                        input.readInt()
                    } catch (_: EOFException) {
                        return
                    }
                    if (count == -1) return
                    require(count in 0..100_000) { "Invalid native compiler argument count: $count" }
                    val stdout = File(input.readString())
                    val stderr = File(input.readString())
                    val arguments = Array(count) { input.readString() }
                    val result = PrintStream(stdout.outputStream(), true, Charsets.UTF_8.name()).use { requestOut ->
                        PrintStream(stderr.outputStream(), true, Charsets.UTF_8.name()).use { requestErr ->
                            System.setOut(requestOut)
                            System.setErr(requestErr)
                            try {
                                // Each request has fresh arguments and diagnostics but shares the application and plugin loaders.
                                K2JVMCompiler().execAndOutputXml(requestErr, services, *arguments)
                            } finally {
                                System.setOut(originalErr)
                                System.setErr(originalErr)
                            }
                        }
                    }
                    output.writeInt(result.code)
                    output.flush()
                }
            }
        } finally {
            Disposer.dispose(disposable)
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    private fun DataInputStream.readString(): String {
        val length = readInt()
        require(length in 0..16_777_216) { "Invalid native compiler argument length: $length" }
        return ByteArray(length).also { readFully(it) }.toString(Charsets.UTF_8)
    }

    @OptIn(ExperimentalCompilerApi::class)
    private class CachedPluginsLoader : PluginsLoader, AutoCloseable {
        private data class Entry(val file: File, val modified: Long, val length: Long)

        private val loaders = LinkedHashMap<List<Entry>, URLClassLoader>(16, 0.75f, true)

        private fun loader(classpath: Collection<String>): URLClassLoader {
            val key = classpath.map { File(it).absoluteFile }.map { Entry(it, it.lastModified(), it.length()) }
            return loaders.getOrPut(key) {
                if (loaders.size >= 16) {
                    val eldest = loaders.entries.iterator()
                    eldest.next().value.close()
                    eldest.remove()
                }
                URLClassLoader(key.map { it.file.toURI().toURL() }.toTypedArray(), K2JVMCompiler::class.java.classLoader)
            }
        }

        override fun loadCompilerPluginRegistrars(
            pluginClasspath: Collection<String>, parentDisposable: Disposable,
        ): List<CompilerPluginRegistrar> = ServiceLoaderLite.loadImplementations(CompilerPluginRegistrar::class.java, loader(pluginClasspath))

        override fun loadCommandLineProcessors(
            pluginClasspath: Collection<String>, parentDisposable: Disposable,
        ): List<CommandLineProcessor> = ServiceLoaderLite.loadImplementations(CommandLineProcessor::class.java, loader(pluginClasspath))

        override fun close() {
            loaders.values.forEach { it.close() }
        }
    }
}
