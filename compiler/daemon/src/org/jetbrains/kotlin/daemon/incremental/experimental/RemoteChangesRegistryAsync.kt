/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import kotlinx.coroutines.experimental.async
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacadeAsync
import org.jetbrains.kotlin.daemon.incremental.toSimpleDirtyData
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry

internal class RemoteChangesRegistryAsync(private val servicesFacade: IncrementalCompilerServicesFacadeAsync) : ChangesRegistry {
    override fun unknownChanges(timestamp: Long) {
        async {
            servicesFacade.unknownChanges(timestamp)
        }
    }

    override fun registerChanges(timestamp: Long, dirtyData: DirtyData) {
        async {
            servicesFacade.registerChanges(timestamp, dirtyData.toSimpleDirtyData())
        }
    }
}