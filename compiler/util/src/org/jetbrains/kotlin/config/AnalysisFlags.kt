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

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalysisFlag internal constructor(private val name: String) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    companion object
}

private operator fun AnalysisFlag.Companion.provideDelegate(instance: Any?, property: KProperty<*>) =
        object : ReadOnlyProperty<Any?, AnalysisFlag> {
            private val flag = AnalysisFlag(property.name)

            override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag = flag
        }

object AnalysisFlags {
    @JvmStatic
    val skipMetadataVersionCheck by AnalysisFlag

    @JvmStatic
    val multiPlatformDoNotCheckImpl by AnalysisFlag
}
