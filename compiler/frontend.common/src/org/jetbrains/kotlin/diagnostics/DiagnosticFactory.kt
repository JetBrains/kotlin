/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer
import java.lang.IllegalArgumentException

abstract class DiagnosticFactory<D : UnboundDiagnostic> protected constructor(
    open var name: String?,
    open val severity: Severity
) {

    open var defaultRenderer: DiagnosticRenderer<D>? = null

    protected constructor(severity: Severity) : this(null, severity)

    @Suppress("UNCHECKED_CAST")
    fun initDefaultRenderer(defaultRenderer: DiagnosticRenderer<*>?) {
        this.defaultRenderer = defaultRenderer as DiagnosticRenderer<D>?
    }

    fun cast(diagnostic: UnboundDiagnostic): D {
        require(!(diagnostic.factory !== this)) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }

    override fun toString(): String {
        return name ?: "<Anonymous DiagnosticFactory>"
    }

    companion object {
        @SafeVarargs
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}