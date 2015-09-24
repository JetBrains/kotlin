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

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.rmi.CompileService
import org.jetbrains.kotlin.rmi.LoopbackNetworkInterface
import org.jetbrains.kotlin.rmi.SOCKET_ANY_FREE_PORT
import java.rmi.server.UnicastRemoteObject


public class RemoteIncrementalCompilationComponentsServer(val base: IncrementalCompilationComponents, val port: Int = SOCKET_ANY_FREE_PORT) : CompileService.RemoteIncrementalCompilationComponents {

    init {
        UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    }

    private val cacheServers = hashMapOf<TargetId, RemoteIncrementalCacheServer>()
    private val lookupTrackerServer by lazy { RemoteLookupTrackerServer(base.getLookupTracker()) }

    override fun getIncrementalCache(target: TargetId): CompileService.RemoteIncrementalCache =
            cacheServers.get(target) ?: {
                val newServer = RemoteIncrementalCacheServer(base.getIncrementalCache(target), port)
                cacheServers.put(target, newServer)
                newServer
            }()

    override fun getLookupTracker(): CompileService.RemoteLookupTracker = lookupTrackerServer
}
