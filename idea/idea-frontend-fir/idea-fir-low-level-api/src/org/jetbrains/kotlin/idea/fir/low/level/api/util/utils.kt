/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo


internal inline fun <T> executeOrReturnDefaultValueOnPCE(defaultValue: T, action: () -> T): T =
    try {
        action()
    } catch (e: ProcessCanceledException) {
        defaultValue
    }

internal val FirElement.isErrorElement
    get() = this is FirDiagnosticHolder

@Suppress("NOTHING_TO_INLINE")
internal inline fun <K, V> MutableMap<K, MutableList<V>>.addValueFor(element: K, value: V) {
    getOrPut(element) { mutableListOf() } += value
}

internal fun IdeaModuleInfo.collectTransitiveDependenciesWithSelf(): List<IdeaModuleInfo> {
    val result = mutableSetOf<IdeaModuleInfo>()
    fun collect(module: IdeaModuleInfo) {
        if (module in result) return
        result += module
        module.dependencies().forEach(::collect)
    }
    collect(this)
    return result.toList()
}