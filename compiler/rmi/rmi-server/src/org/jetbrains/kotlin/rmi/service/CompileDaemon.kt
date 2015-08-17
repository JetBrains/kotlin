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

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.rmi.COMPILE_DAEMON_DEFAULT_PORT
import org.jetbrains.kotlin.rmi.CompilerId
import org.jetbrains.kotlin.rmi.DaemonOptions
import org.jetbrains.kotlin.rmi.propParseFilter
import org.jetbrains.kotlin.service.CompileServiceImpl
import java.rmi.RMISecurityManager
import java.rmi.registry.LocateRegistry
import kotlin.platform.platformStatic

public class CompileDaemon {

    companion object {

        platformStatic public fun main(args: Array<String>) {

            val compilerId = CompilerId()
            val daemonOptions = DaemonOptions()
            val filteredArgs = args.asIterable().propParseFilter(compilerId, daemonOptions)

            if (filteredArgs.any()) {
                println("usage: <daemon> <compilerId options> <daemon options>")
                throw IllegalArgumentException("Unknown arguments")
            }

            // TODO: find minimal set of permissions and restore security management
//            if (System.getSecurityManager() == null)
//                System.setSecurityManager (RMISecurityManager())
//
//            setDaemonPpermissions(daemonOptions.port)

            val registry = LocateRegistry.createRegistry(daemonOptions.port);
            val compiler = K2JVMCompiler()

            val server = CompileServiceImpl(registry, compiler, compilerId, daemonOptions)

            if (daemonOptions.startEcho.isNotEmpty())
                println(daemonOptions.startEcho)
        }
    }
}