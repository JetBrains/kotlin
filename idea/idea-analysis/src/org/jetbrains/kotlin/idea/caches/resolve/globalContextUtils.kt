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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.ExceptionTracker
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.diagnostic.Logger

fun GlobalContextImpl.withCompositeExceptionTrackerUnderSameLock(): GlobalContextImpl {
    val newExceptionTracker = CompositeExceptionTracker(this.exceptionTracker)
    val newStorageManager = LockBasedStorageManager.createDelegatingWithSameLock(this.storageManager, newExceptionTracker)
    return GlobalContextImpl(newStorageManager, newExceptionTracker)
}

private class CompositeExceptionTracker(val delegate: ExceptionTracker) : ExceptionTracker() {
    override fun getModificationCount(): Long {
        return super.getModificationCount() + delegate.getModificationCount()
    }
}

private class ExceptionTrackerWithProcessCanceledReport() : ExceptionTracker() {
    override fun handleException(throwable: Throwable): RuntimeException {
        if (throwable is ProcessCanceledException) {
            LOG.info("ProcessCancelException was thrown while analyzing libraries. Cache has to be rebuilt.")
        }
        throw super.handleException(throwable)
    }


    default object {
        val LOG = Logger.getInstance(javaClass<ExceptionTrackerWithProcessCanceledReport>())
    }
}

public fun GlobalContext(logProcessCanceled: Boolean): GlobalContextImpl {
    val tracker = if (logProcessCanceled) ExceptionTrackerWithProcessCanceledReport() else ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
}
