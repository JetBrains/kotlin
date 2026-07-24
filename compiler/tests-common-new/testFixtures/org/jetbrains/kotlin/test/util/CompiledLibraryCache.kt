/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.ApplicationEnvironmentDisposer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CompiledLibraryCache : Disposable {
    private val cache = ConcurrentHashMap<String, File>()

    init {
        Disposer.register(ApplicationEnvironmentDisposer.ROOT_DISPOSABLE, this)
    }

    fun getOrCompile(key: String, compile: () -> File): File {
        return cache.computeIfAbsent(key) { compile() }
    }

    override fun dispose() {
        cache.clear()
    }
}
