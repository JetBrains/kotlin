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

package org.jetbrains.kotlin.cli.common.arguments

abstract class Freezable {
    protected fun checkFrozen() {
        if (frozen) throw IllegalStateException("Instance of ${this::class} is frozen")
    }

    private var frozen: Boolean = false

    protected open fun copyOf(): Freezable = copyBean(this)

    internal fun copyOfInternal(): Freezable = copyOf()
    internal fun getInstanceWithFreezeStatus(value: Boolean) = if (value == frozen) this else copyOf().apply { frozen = value }

    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Please use type safe extension functions")
    fun frozen() = getInstanceWithFreezeStatus(true)

    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Please use type safe extension functions")
    fun unfrozen() = getInstanceWithFreezeStatus(false)
}

@Suppress("UNCHECKED_CAST")
fun <T : Freezable> T.copyOf(): T = copyOfInternal() as T

@Suppress(
    "UNCHECKED_CAST",
    "EXTENSION_SHADOWED_BY_MEMBER", // It's false positive shadowed warning KT-21598
    "unused" //used from kotlin plugin
)
fun <T : Freezable> T.frozen(): T = getInstanceWithFreezeStatus(true) as T
@Suppress(
    "UNCHECKED_CAST",
    "EXTENSION_SHADOWED_BY_MEMBER" // It's false positive shadowed warning KT-21598
)
fun <T : Freezable> T.unfrozen(): T = getInstanceWithFreezeStatus(false) as T
