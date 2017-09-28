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

package org.jetbrains.kotlin.utils

enum class ReportLevel(val description: String) {
    IGNORE("ignore"),
    WARN("warn"),
    STRICT("strict"),
    ;

    companion object {
        fun findByDescription(description: String?): ReportLevel? = values().firstOrNull { it.description == description }
    }

    val isWarning: Boolean get() = this == ReportLevel.WARN
    val isIgnore: Boolean get() = this == ReportLevel.IGNORE
}

data class Jsr305State(
        val global: ReportLevel,
        val migration: ReportLevel?,
        val user: Map<String, ReportLevel>
) {
    val description: Array<String> by lazy {
        val result = mutableListOf<String>()
        result.add(global.description)

        migration?.let { result.add("under-migration:${it.description}") }

        user.forEach {
            result.add("@${it.key}:${it.value.description}")
        }

        result.toTypedArray()
    }

    val disabled: Boolean get() = this === DISABLED

    companion object {
        @JvmField
        val DEFAULT: Jsr305State = Jsr305State(ReportLevel.WARN, null, emptyMap())

        @JvmField
        val DISABLED: Jsr305State = Jsr305State(ReportLevel.IGNORE, ReportLevel.IGNORE, emptyMap())

        @JvmField
        val STRICT: Jsr305State = Jsr305State(ReportLevel.STRICT, ReportLevel.STRICT, emptyMap())
    }
}
