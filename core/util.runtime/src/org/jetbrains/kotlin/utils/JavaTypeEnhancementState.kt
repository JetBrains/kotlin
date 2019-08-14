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

class JavaTypeEnhancementState(
    val globalJsr305Level: ReportLevel,
    val migrationLevelForJsr305: ReportLevel?,
    val userDefinedLevelForSpecificJsr305Annotation: Map<String, ReportLevel>,
    val enableCompatqualCheckerFrameworkAnnotations: Boolean = COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS_SUPPORT_DEFAULT_VALUE
) {
    val description: Array<String> by lazy {
        val result = mutableListOf<String>()
        result.add(globalJsr305Level.description)

        migrationLevelForJsr305?.let { result.add("under-migration:${it.description}") }

        userDefinedLevelForSpecificJsr305Annotation.forEach {
            result.add("@${it.key}:${it.value.description}")
        }

        result.toTypedArray()
    }

    val disabledJsr305: Boolean get() = this === DISABLED_JSR_305

    companion object {
        const val COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS_SUPPORT_DEFAULT_VALUE = true

        @JvmField
        val DEFAULT: JavaTypeEnhancementState = JavaTypeEnhancementState(ReportLevel.WARN, null, emptyMap())

        @JvmField
        val DISABLED_JSR_305: JavaTypeEnhancementState = JavaTypeEnhancementState(ReportLevel.IGNORE, ReportLevel.IGNORE, emptyMap())

        @JvmField
        val STRICT: JavaTypeEnhancementState = JavaTypeEnhancementState(ReportLevel.STRICT, ReportLevel.STRICT, emptyMap())
    }
}
