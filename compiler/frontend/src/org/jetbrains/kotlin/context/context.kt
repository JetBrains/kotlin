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

package org.jetbrains.kotlin.context

import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager

public trait GlobalContext {
    public val storageManager: StorageManager
    public val exceptionTracker: ExceptionTracker
}

public open class SimpleGlobalContext(
        override val storageManager: StorageManager,
        override val exceptionTracker: ExceptionTracker
) : GlobalContext

public open class GlobalContextImpl(
        storageManager: LockBasedStorageManager,
        exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

public fun GlobalContext(): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
}

deprecated("Used temporarily while we are in transition from to lazy resolve")
public open class TypeLazinessToken {
    deprecated("Used temporarily while we are in transition from to lazy resolve")
    public open fun isLazy(): Boolean = false
}

deprecated("Used temporarily while we are in transition from to lazy resolve")
public class LazyResolveToken : TypeLazinessToken() {
    deprecated("Used temporarily while we are in transition from to lazy resolve")
    override fun isLazy() = true
}
