/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry

internal class RemoteChangesRegistry(private val servicesFacade: IncrementalCompilerServicesFacade) : ChangesRegistry {
    override fun unknownChanges(timestamp: Long) {
        servicesFacade.unknownChanges(timestamp)
    }

    override fun registerChanges(timestamp: Long, dirtyData: DirtyData) {
        servicesFacade.registerChanges(timestamp, dirtyData.toSimpleDirtyData())
    }
}