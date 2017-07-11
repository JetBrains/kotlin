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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.RmiFriendlyCompilationCanceledException
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.isProcessCanceledException
import java.rmi.server.UnicastRemoteObject
import kotlin.reflect.full.allSuperclasses


open class CompilerCallbackServicesFacadeServer(
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val compilationCanceledStatus: CompilationCanceledStatus? = null,
        port: Int = SOCKET_ANY_FREE_PORT
) : CompilerCallbackServicesFacade,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
{
    override fun hasIncrementalCaches(): Boolean = incrementalCompilationComponents != null

    override fun hasLookupTracker(): Boolean = incrementalCompilationComponents != null

    override fun hasCompilationCanceledStatus(): Boolean = compilationCanceledStatus != null

    // TODO: consider replacing NPE with other reporting, although NPE here means most probably incorrect usage

    override fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> = incrementalCompilationComponents!!.getIncrementalCache(target).getObsoletePackageParts()

    override fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> = incrementalCompilationComponents!!.getIncrementalCache(target).getObsoleteMultifileClasses()

    override fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? = incrementalCompilationComponents!!.getIncrementalCache(target).getStableMultifileFacadeParts(internalName)

    override fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? = incrementalCompilationComponents!!.getIncrementalCache(target).getPackagePartData(partInternalName)

    override fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? = incrementalCompilationComponents!!.getIncrementalCache(target).getModuleMappingData()

    override fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
        incrementalCompilationComponents!!.getIncrementalCache(target).registerInline(fromPath, jvmSignature, toPath)
    }

    override fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String = incrementalCompilationComponents!!.getIncrementalCache(target).getClassFilePath(internalClassName)

    override fun incrementalCache_close(target: TargetId) {
        incrementalCompilationComponents!!.getIncrementalCache(target).close()
    }

    override fun lookupTracker_requiresPosition() = incrementalCompilationComponents!!.getLookupTracker().requiresPosition

    override fun lookupTracker_record(lookups: Collection<LookupInfo>) {
        val lookupTracker = incrementalCompilationComponents!!.getLookupTracker()

        for (it in lookups) {
            lookupTracker.record(it.filePath, it.position, it.scopeFqName, it.scopeKind, it.name)
        }
    }

    private val lookupTracker_isDoNothing: Boolean = incrementalCompilationComponents?.getLookupTracker() === LookupTracker.DO_NOTHING

    override fun lookupTracker_isDoNothing(): Boolean = lookupTracker_isDoNothing

    override fun compilationCanceledStatus_checkCanceled(): Void? {
        try {
            compilationCanceledStatus!!.checkCanceled()
            return null
        }
        catch (e: Exception) {
            // avoid passing exceptions that may have different serialVersionUID on across rmi border
            // removing dependency from openapi (this is obsolete part anyway, and will be removed soon)
            if (e.isProcessCanceledException())
                throw RmiFriendlyCompilationCanceledException()
            else throw e
        }
    }
}
