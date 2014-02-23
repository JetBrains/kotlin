/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.context

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.storage.ExceptionTracker
import org.jetbrains.jet.storage.LockBasedStorageManager

trait GlobalContext {
    val storageManager: StorageManager
    val exceptionTracker: ExceptionTracker
}

open class SimpleGlobalContext(
        override val storageManager: StorageManager,
        override val exceptionTracker: ExceptionTracker
) : GlobalContext

open class GlobalContextImpl(
        storageManager: LockBasedStorageManager,
        exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

fun GlobalContext(): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
}