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

import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Freezable {
    protected open inner class FreezableVar<T>(private var value: T) : ReadWriteProperty<Any, T>, Serializable {
        override fun getValue(thisRef: Any, property: KProperty<*>) = value

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            if (frozen) throw IllegalStateException("Instance of ${this::class} is frozen")
            this.value = value
        }
    }

    protected inner class NullableStringFreezableVar(value: String?) : FreezableVar<String?>(value) {
        private val defaultValue = value

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
            super.setValue(thisRef, property, if (value.isNullOrEmpty()) defaultValue else value)
        }
    }

    private var frozen: Boolean = false

    internal fun getInstanceWithFreezeStatus(value: Boolean) = if (value == frozen) this else copyBean(this).apply { frozen = value }
}

@Suppress("UNCHECKED_CAST")
fun <T : Freezable> T.frozen(): T = getInstanceWithFreezeStatus(true) as T
@Suppress("UNCHECKED_CAST")
fun <T : Freezable> T.unfrozen(): T = getInstanceWithFreezeStatus(false) as T
