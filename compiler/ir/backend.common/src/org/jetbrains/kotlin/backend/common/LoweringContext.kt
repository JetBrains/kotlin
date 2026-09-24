/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.config.reportLog
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory

/**
 * A context that is used to pass data to both first (before IR serialization) and second (after IR deserialization) stage compiler
 * lowerings.
 */
interface LoweringContext : LoggingContext, ErrorReportingContext {
    val configuration: CompilerConfiguration
    val symbols: PreSerializationSymbols
    val irBuiltIns: IrBuiltIns
    val irFactory: IrFactory
    val sharedVariablesManager: SharedVariablesManager

    /**
     * Whether the inliner has to narrow values produced at an erased generic type back to their substituted types with a
     * checked `CAST` rather than an `IMPLICIT_CAST` (see [org.jetbrains.kotlin.ir.util.crossesErasureBoundary]).
     *
     * This is needed by a backend which checks such casts at runtime (Wasm), because the inliner partially runs before IR
     * serialization, and an attribute on an `IMPLICIT_CAST` doesn't survive it. A `CAST` does, and every backend checks it.
     * This is preferred over serializing the attribute: it needs no klib format change, and klibs stay readable by compilers
     * which don't know about erasure boundaries. Any backend which wants these checks can enable them by overriding this.
     *
     * Like the JVM's `checkcast`, this `CAST` lets `null` through (it casts to the nullable type, with an `IMPLICIT_CAST` to
     * the non-null type on top): e.g. a companion block initializer may pass a companion object which isn't created yet
     * (`null`) as the receiver of an inline function.
     */
    val checkErasureBoundaryCastsInInliner: Boolean
        get() = false

    override fun log(message: String) {
        configuration.reportLog(message)
    }
}
