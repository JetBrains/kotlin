/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacadeAsync
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.modules.TargetId

interface CompilerCallbackServicesFacadeServerSide : CompilerCallbackServicesFacadeAsync, CompilerServicesFacadeBaseServerSide {

    class HasIncrementalCachesMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.hasIncrementalCaches())
    }

    class HasLookupTrackerMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.hasLookupTracker())
    }

    class HasCompilationCanceledStatusMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.hasCompilationCanceledStatus())
    }

    // ----------------------------------------------------
    // IncrementalCache
    class IncrementalCache_getObsoletePackagePartsMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getObsoletePackageParts(target))
    }

    class IncrementalCache_getObsoleteMultifileClassFacadesMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getObsoleteMultifileClassFacades(target))
    }

    class IncrementalCache_getPackagePartDataMessage(val target: TargetId, val partInternalName: String) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getPackagePartData(target, partInternalName))
    }

    class IncrementalCache_getModuleMappingDataMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getModuleMappingData(target))
    }

    class IncrementalCache_registerInlineMessage(
        val target: TargetId,
        val fromPath: String,
        val jvmSignature: String,
        val toPath: String
    ) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            server.incrementalCache_registerInline(target, fromPath, jvmSignature, toPath)
    }

    class IncrementalCache_getClassFilePathMessage(val target: TargetId, val internalClassName: String) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getClassFilePath(target, internalClassName))
    }

    class IncrementalCache_closeMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            server.incrementalCache_close(target)
    }

    class IncrementalCache_getMultifileFacadePartsMessage(val target: TargetId, val internalName: String) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.incrementalCache_getMultifileFacadeParts(target, internalName))
    }

    // ----------------------------------------------------
    // LookupTracker

    class LookupTracker_requiresPositionMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) {
            server.lookupTracker_requiresPosition()
        }
    }

    class LookupTracker_recordMessage(val lookups: Collection<LookupInfo>) : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.lookupTracker_record(lookups))
    }

    class LookupTracker_isDoNothingMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.lookupTracker_isDoNothing())
    }

    // ----------------------------------------------------
    // CompilationCanceledStatus
    class CompilationCanceledStatus_checkCanceledMessage : Message<CompilerCallbackServicesFacadeServerSide>() {
        override suspend fun processImpl(server: CompilerCallbackServicesFacadeServerSide, printObject: (Any?) -> Unit) {
            server.compilationCanceledStatus_checkCanceled()
        }
    }

}