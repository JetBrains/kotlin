/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.storage

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException
import org.jetbrains.kotlin.utils.isProcessCanceledException
import org.jetbrains.kotlin.utils.rethrow
import java.util.concurrent.atomic.AtomicLong

open class ExceptionTracker : ModificationTracker, LockBasedStorageManager.ExceptionHandlingStrategy {
    private val cancelledTracker: AtomicLong = AtomicLong()

    override fun handleException(throwable: Throwable): RuntimeException {
        // should not increment counter when ReenteringLazyValueComputationException is thrown since it implements correct frontend behaviour
        if (throwable !is ReenteringLazyValueComputationException) {
            if (!throwable.isProcessCanceledException() || CacheResetOnProcessCanceled.enabled) {
                incCounter()
            }
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
        return this::class.java.name + ": " + modificationCount
    }
}

object CacheResetOnProcessCanceled {
    private const val PROPERTY = "kotlin.internal.cacheResetOnProcessCanceled"
    private const val DEFAULT_VALUE = true

    var enabled: Boolean
        get() = PropertiesComponent.getInstance()?.getBoolean(PROPERTY, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) {
            PropertiesComponent.getInstance()?.setValue(PROPERTY, value, DEFAULT_VALUE)
        }
}
