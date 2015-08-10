/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.rmi.service

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.UsageCollector
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.rmi.CompilerFacade
import org.jetbrains.kotlin.rmi.RemoteOutputStream
import java.io.PrintStream
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.TimeUnit


class CompilerFacadeImpl<Compiler: CLICompiler<*>>(val compiler: Compiler) : CompilerFacade, UnicastRemoteObject() {

    public class IncrementalCompilationComponentsImpl(val idToCache: Map<String, CompilerFacade.RemoteIncrementalCache>): IncrementalCompilationComponents {
        override fun getIncrementalCache(moduleId: String): IncrementalCache = idToCache[moduleId]!!
        override fun getUsageCollector(): UsageCollector = UsageCollector.DO_NOTHING
    }

    private fun createCompileServices(incrementalCaches: Map<String, CompilerFacade.RemoteIncrementalCache>): Services =
        Services.Builder()
                .register(javaClass<IncrementalCompilationComponents>(), IncrementalCompilationComponentsImpl(incrementalCaches))
//                .register(javaClass<CompilationCanceledStatus>(), object: CompilationCanceledStatus {
//                    override fun checkCanceled(): Unit = if (context.getCancelStatus().isCanceled()) throw CompilationCanceledException()
//                })
                .build()

    fun usedMemoryKb(): Long {
        System.gc()
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / 1024
    }

    fun<R> checked(args: Array<String>, body: () -> R): R {
        try {
            if (args.none())
                throw IllegalArgumentException("Error: empty arguments list.")
            println("Starting compilation with args: " + args.joinToString(" "))
            val startMem = usedMemoryKb()
            val startTime = System.nanoTime()
            val res = body()
            val endTime = System.nanoTime()
            val endMem = usedMemoryKb()
            println("Done")
            println("Elapsed time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
            println("Used memory: $endMem kb (${"%+d".format(endMem - startMem)} kb)")
            return res
        }
        catch (e: Exception) {
            println("Error: $e")
            throw e
        }
    }

    override fun remoteCompile(args: Array<String>, errStream: RemoteOutputStream, outputFormat: CompilerFacade.OutputFormat): Int =
        checked(args) {
            val strm = RemoteOutputStreamClient(errStream)
            val printStrm = PrintStream(strm)
            when (outputFormat) {
                CompilerFacade.OutputFormat.PLAIN -> compiler.exec(printStrm, *args)
                CompilerFacade.OutputFormat.XML -> compiler.execAndOutputXml(printStrm, Services.EMPTY, *args)
            }.code
        }

    override fun remoteIncrementalCompile(args: Array<String>, caches: Map<String, CompilerFacade.RemoteIncrementalCache>, errStream: RemoteOutputStream, outputFormat: CompilerFacade.OutputFormat): Int =
        checked(args) {
            val strm = RemoteOutputStreamClient(errStream)
            val printStrm = PrintStream(strm)
            when (outputFormat) {
                CompilerFacade.OutputFormat.PLAIN -> throw NotImplementedError("Only XML output is supported in remote incremental compilation")
                CompilerFacade.OutputFormat.XML -> compiler.execAndOutputXml(printStrm, createCompileServices(caches), *args)
            }.code
        }
}
