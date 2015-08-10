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
import org.jetbrains.kotlin.rmi.CompilerFacade
import java.rmi.RMISecurityManager
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject
import kotlin.platform.platformStatic

public class CompileServer {

    companion object {
        platformStatic public fun main(args: Array<String>) {
            if (System.getSecurityManager() == null)
                System.setSecurityManager (RMISecurityManager())

            val registry = LocateRegistry.createRegistry(17031);

            val server = CompilerFacadeImpl(K2JVMCompiler())
            try {
                UnicastRemoteObject.unexportObject(server, false)
            }
            catch (e: java.rmi.NoSuchObjectException) {
                // ignoring if object already exported
            }

            val stub = UnicastRemoteObject.exportObject(server, 0) as CompilerFacade
            registry.rebind ("KotlinJvmCompilerService", stub);
        }
    }
}