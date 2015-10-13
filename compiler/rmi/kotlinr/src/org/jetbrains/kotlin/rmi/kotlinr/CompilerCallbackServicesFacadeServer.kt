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
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.rmi.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.rmi.LoopbackNetworkInterface
import org.jetbrains.kotlin.rmi.SOCKET_ANY_FREE_PORT


public class CompilerCallbackServicesFacadeServer(
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val compilationCancelledStatus: CompilationCanceledStatus? = null,
        port: Int = SOCKET_ANY_FREE_PORT
) : CompilerCallbackServicesFacade,
    java.rmi.server.UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
{
    override fun hasIncrementalCaches(): Boolean = incrementalCompilationComponents != null

    override fun hasLookupTracker(): Boolean = incrementalCompilationComponents != null

    override fun hasCompilationCanceledStatus(): Boolean = compilationCancelledStatus != null

    // TODO: consider replacing NPE with other reporting, although NPE here means most probably incorrect usage

    override fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> = incrementalCompilationComponents!!.getIncrementalCache(target).getObsoletePackageParts()

    override fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> = incrementalCompilationComponents!!.getIncrementalCache(target).getObsoleteMultifileClasses()

    override fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? = incrementalCompilationComponents!!.getIncrementalCache(target).getStableMultifileFacadeParts(internalName)

    override fun incrementalCache_getMultifileFacade(target: TargetId, partInternalName: String): String? = incrementalCompilationComponents!!.getIncrementalCache(target).getMultifileFacade(partInternalName)

    override fun incrementalCache_getPackagePartData(target: TargetId, fqName: String): JvmPackagePartProto? = incrementalCompilationComponents!!.getIncrementalCache(target).getPackagePartData(fqName)

    override fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? = incrementalCompilationComponents!!.getIncrementalCache(target).getModuleMappingData()

    override fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
        incrementalCompilationComponents!!.getIncrementalCache(target).registerInline(fromPath, jvmSignature, toPath)
    }

    override fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String = incrementalCompilationComponents!!.getIncrementalCache(target).getClassFilePath(internalClassName)

    override fun incrementalCache_close(target: TargetId) {
        incrementalCompilationComponents!!.getIncrementalCache(target).close()
    }

    override fun lookupTracker_record(lookupContainingFile: String, lookupLine: Int?, lookupColumn: Int?, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        incrementalCompilationComponents!!.getLookupTracker().record(lookupContainingFile, lookupLine, lookupColumn, scopeFqName, scopeKind, name)
    }

    private val lookupTracker_isDoNothing: Boolean = incrementalCompilationComponents != null && incrementalCompilationComponents.getLookupTracker() == LookupTracker.DO_NOTHING

    override fun lookupTracker_isDoNothing(): Boolean = lookupTracker_isDoNothing

    override fun compilationCanceledStatus_checkCanceled() {
        compilationCancelledStatus!!.checkCanceled()
    }
}
