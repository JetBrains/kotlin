/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.storage

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException
import java.util.concurrent.atomic.AtomicLong
import org.jetbrains.kotlin.utils.rethrow

open class ExceptionTracker : ModificationTracker, LockBasedStorageManager.ExceptionHandlingStrategy {
    private val cancelledTracker: AtomicLong = AtomicLong()

    override fun handleException(throwable: Throwable): RuntimeException {
        // should not increment counter when ReenteringLazyValueComputationException is thrown since it implements correct frontend behaviour
        if (throwable !is ReenteringLazyValueComputationException) {
            incCounter()
        }
        throw rethrow(throwable)
    }

    private fun incCounter() {
        cancelledTracker.andIncrement
    }

    override fun getModificationCount(): Long {
        return cancelledTracker.get()
    }

    override fun toString(): String {
        return javaClass.name + ": " + modificationCount
    }
}
