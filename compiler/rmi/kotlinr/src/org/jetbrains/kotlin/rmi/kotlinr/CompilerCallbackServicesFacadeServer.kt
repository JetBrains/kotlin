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

import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.rmi.*
import java.rmi.server.UnicastRemoteObject

public class RemoteIncrementalCompilationComponentsServer(val base: IncrementalCompilationComponents, val port: Int = SOCKET_ANY_FREE_PORT) : RemoteIncrementalCompilationComponents {

    init {
        UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    }

    private val cacheServers = hashMapOf<TargetId, RemoteIncrementalCacheServer>()
    private val lookupTrackerServer by lazy { RemoteLookupTrackerServer(base.getLookupTracker()) }

    override fun getIncrementalCache(target: TargetId): RemoteIncrementalCache =
            cacheServers.get(target) ?: {
                val newServer = RemoteIncrementalCacheServer(base.getIncrementalCache(target), port)
                cacheServers.put(target, newServer)
                newServer
            }()

    override fun getLookupTracker(): RemoteLookupTracker = lookupTrackerServer
}


public class RemoteIncrementalCacheServer(val cache: IncrementalCache, port: Int = SOCKET_ANY_FREE_PORT) : RemoteIncrementalCache {

    init {
        UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    }

    override fun getObsoletePackageParts(): Collection<String> = cache.getObsoletePackageParts()

    override fun getObsoleteMultifileClassFacades(): Collection<String> = cache.getObsoleteMultifileClasses()

    override fun getMultifileFacadeParts(internalName: String): Collection<String>? = cache.getStableMultifileFacadeParts(internalName)

    override fun getMultifileFacade(partInternalName: String): String? = cache.getMultifileFacade(partInternalName)

    override fun getPackagePartData(fqName: String): JvmPackagePartProto? = cache.getPackagePartData(fqName)

    override fun getModuleMappingData(): ByteArray? = cache.getModuleMappingData()

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        cache.registerInline(fromPath, jvmSignature, toPath)
    }

    override fun getClassFilePath(internalClassName: String): String = cache.getClassFilePath(internalClassName)

    override fun close() {
        cache.close()
    }

    public fun disconnect() {
        UnicastRemoteObject.unexportObject(this, true)
    }
}

public class RemoteLookupTrackerServer(val base: LookupTracker, port: Int = SOCKET_ANY_FREE_PORT) : RemoteLookupTracker {

    init {
        UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    }

    override fun record(lookupContainingFile: String, lookupLine: Int?, lookupColumn: Int?, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        base.record(lookupContainingFile, lookupLine, lookupColumn, scopeFqName, scopeKind, name)
    }

    private val _isDoNothing: Boolean = base == LookupTracker.DO_NOTHING
    override fun isDoNothing(): Boolean = _isDoNothing
}


public class RemoteCompilationCanceledStatusServer(val base: CompilationCanceledStatus, port: Int = SOCKET_ANY_FREE_PORT) : RemoteCompilationCanceledStatus {

    init {
        UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    }

    override fun checkCanceled() {
        base.checkCanceled()
    }
}