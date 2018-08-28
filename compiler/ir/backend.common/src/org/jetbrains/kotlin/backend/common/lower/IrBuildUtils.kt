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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol

abstract class SymbolWithIrBuilder<out S: IrSymbol, out D: IrDeclaration> {

    protected abstract fun buildSymbol(): S

    protected open fun doInitialize() { }

    protected abstract fun buildIr(): D

    val symbol by lazy { buildSymbol() }

    private val builtIr by lazy { buildIr() }
    private var initialized: Boolean = false

    fun initialize() {
        doInitialize()
        initialized = true
    }

    val ir: D
        get() {
            if (!initialized)
                throw Error("Access to IR before initialization")
            return builtIr
        }
}
