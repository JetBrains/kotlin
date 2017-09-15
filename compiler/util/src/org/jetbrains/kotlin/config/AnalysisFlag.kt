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

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalysisFlag<out T> internal constructor(
        private val name: String,
        val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    private class Flag<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag

        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, false)
        }

        object Jsr305StateWarnByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, Jsr305State.DEFAULT)
        }
    }

    companion object Flags {
        @JvmStatic
        val skipMetadataVersionCheck by Flag.Boolean

        @JvmStatic
        val multiPlatformDoNotCheckActual by Flag.Boolean

        @JvmStatic
        val jsr305 by Flag.Jsr305StateWarnByDefault
    }
}
