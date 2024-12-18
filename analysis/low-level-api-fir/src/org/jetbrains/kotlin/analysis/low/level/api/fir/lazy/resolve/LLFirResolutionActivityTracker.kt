/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.platform.resolution.KaResolutionActivityTracker

/**
 * The service use site guarantees that all [beforeLazyResolve] and [afterLazyResolve] calls are paired,
 * so for each [beforeLazyResolve] call where will be the following [afterLazyResolve]. **Nested calls are allowed**.
 */
@OptIn(KaIdeApi::class)
internal class LLFirResolutionActivityTracker : KaResolutionActivityTracker {
    private val blockCounter = ThreadLocal.withInitial { BlockCounter() }

    fun beforeLazyResolve() {
        blockCounter.get().enter()
    }

    fun afterLazyResolve() {
        blockCounter.get().exit()
    }

    override val isKotlinResolutionActive: Boolean
        get() = blockCounter.get().isInside

    private class BlockCounter {
        private var count = 0

        fun enter() {
            ++count
        }

        fun exit() {
            --count
        }

        /**
         * The service guarantees that all [beforeLazyResolve] and [afterLazyResolve]
         * are paired, so 0 means there is no resolver on the stack, and more than one means ongoing resolution.
         */
        val isInside: Boolean get() = count > 0
    }

    companion object {
        fun getInstance(): LLFirResolutionActivityTracker? {
            return ApplicationManager.getApplication().serviceOrNull<KaResolutionActivityTracker>() as? LLFirResolutionActivityTracker
        }
    }
}