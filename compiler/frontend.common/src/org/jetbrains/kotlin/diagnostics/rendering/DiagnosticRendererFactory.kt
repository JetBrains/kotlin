/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderer
import java.util.LinkedHashSet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun interface DiagnosticRendererFactory {
    operator fun invoke(diagnostic: KtDiagnostic): KtDiagnosticRenderer?
}

abstract class BaseDiagnosticRendererFactory : DiagnosticRendererFactory {
    override operator fun invoke(diagnostic: KtDiagnostic): KtDiagnosticRenderer? {
        val factory = diagnostic.factory
        @Suppress("UNCHECKED_CAST")
        return MAP[factory]
    }

    abstract val MAP: KtDiagnosticFactoryToRendererMap
}

object RootDiagnosticRendererFactory : DiagnosticRendererFactory {
    private val _factories: LinkedHashSet<DiagnosticRendererFactory> = linkedSetOf()
    val factories: Set<DiagnosticRendererFactory>
        get() = _factories

    private val lock = ReentrantLock()

    override operator fun invoke(diagnostic: KtDiagnostic): KtDiagnosticRenderer = lock.withLock {
        for (factory in _factories) {
            val renderer = factory(diagnostic)
            if (renderer != null) return renderer
        }
        diagnostic.factory.ktRenderer
    }

    fun registerFactory(factory: DiagnosticRendererFactory) {
        lock.withLock {
            _factories.add(factory)
        }
    }
}
