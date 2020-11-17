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
package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer
import java.lang.IllegalArgumentException
import java.lang.SafeVarargs

abstract class DiagnosticFactory<D : Diagnostic> protected constructor(
    open var name: String?,
    open val severity: Severity
) {

    open var defaultRenderer: DiagnosticRenderer<D>? = null

    protected constructor(severity: Severity) : this(null, severity)

    fun cast(diagnostic: Diagnostic): D {
        require(!(diagnostic.factory !== this)) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }

    override fun toString(): String {
        return name ?: "<Anonymous DiagnosticFactory>"
    }

    companion object {
        @SafeVarargs
        fun <D : Diagnostic> cast(diagnostic: Diagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        fun <D : Diagnostic> cast(diagnostic: Diagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}