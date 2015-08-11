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

package org.jetbrains.kotlin.rmi.kotlinr

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.rmi.CompilerFacade
import java.io.OutputStream
import java.rmi.registry.LocateRegistry
import kotlin.platform.platformStatic

public class KotlinCompilerClient {

    companion object {

        public fun connectToCompilerServer(): CompilerFacade? {
            val compilerObj = LocateRegistry.getRegistry("localhost", 17031).lookup("KotlinJvmCompilerService")
            if (compilerObj == null)
                println("Unable to find compiler service")
            else {
                val compiler = compilerObj as? CompilerFacade
                if (compiler == null)
                    println("Unable to cast compiler service: ${compilerObj.javaClass}")
                return compiler
            }
            return null
        }

        public fun incrementalCompile(compiler: CompilerFacade, args: Array<String>, caches: Map<String, IncrementalCache>, out: OutputStream): Int {

            val outStrm = RemoteOutputStreamServer(out)
            val cacheServers = hashMapOf<String, RemoteIncrementalCacheServer>()
            try {
                caches.forEach { cacheServers.put( it.getKey(), RemoteIncrementalCacheServer( it.getValue())) }
                val res = compiler.remoteIncrementalCompile(args, cacheServers, outStrm, CompilerFacade.OutputFormat.XML)
                return res
            }
            finally {
                cacheServers.forEach { it.getValue().disconnect() }
                outStrm.disconnect()
            }
        }

        platformStatic public fun main(vararg args: String) {
            connectToCompilerServer()?.let {
                    println("Executing daemon compilation with args: " + args.joinToString(" "))
                    val outStrm = RemoteOutputStreamServer(System.out)
                    try {
                        val res = it.remoteCompile(args, outStrm, CompilerFacade.OutputFormat.PLAIN)
                        println("Compilation result code: $res")
                    }
                    finally {
                        outStrm.disconnect()
                    }
                }
            }
    }
}

