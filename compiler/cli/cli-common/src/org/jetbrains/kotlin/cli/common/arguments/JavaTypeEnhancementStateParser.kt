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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel

class JavaTypeEnhancementStateParser(private val collector: MessageCollector) {
    fun parse(
        jsr305Args: Array<String>?,
        supportCompatqualCheckerFrameworkAnnotations: String?,
        jspecifyState: String?
    ): JavaTypeEnhancementState {
        val jsr305State = parseJsr305State(jsr305Args)

        val enableCompatqualCheckerFrameworkAnnotations = when (supportCompatqualCheckerFrameworkAnnotations) {
            "enable" -> true
            "disable" -> false
            null -> null
            else -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unrecognized -Xsupport-compatqual-checker-framework-annotations option: $supportCompatqualCheckerFrameworkAnnotations. Possible values are 'enable'/'disable'"
                )
                null
            }
        }

        val jspecifyReportLevel = parseJspecifyReportLevel(jspecifyState)

        val state = JavaTypeEnhancementState(
            jsr305State.global ?: ReportLevel.WARN, jsr305State.migration, jsr305State.usedDefined,
            enableCompatqualCheckerFrameworkAnnotations =
            enableCompatqualCheckerFrameworkAnnotations
                ?: JavaTypeEnhancementState.COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS_SUPPORT_DEFAULT_VALUE,
            jspecifyReportLevel = jspecifyReportLevel
        )
        return if (state == JavaTypeEnhancementState.DISABLED_JSR_305) JavaTypeEnhancementState.DISABLED_JSR_305 else state
    }


    private fun parseJspecifyReportLevel(jspecifyState: String?): ReportLevel {
        if (jspecifyState == null) return JavaTypeEnhancementState.DEFAULT_REPORT_LEVEL_FOR_JSPECIFY
        val reportLevel = ReportLevel.findByDescription(jspecifyState)

        if (reportLevel == null) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Unrecognized -Xjspecify-annotations option: $jspecifyState. Possible values are 'disable'/'warn'/'strict'"
            )
            return JavaTypeEnhancementState.DEFAULT_REPORT_LEVEL_FOR_JSPECIFY
        }

        return reportLevel
    }

    private data class Jsr305State(
        val global: ReportLevel?,
        val migration: ReportLevel?,
        val usedDefined: Map<String, ReportLevel>
    )

    private fun parseJsr305State(args: Array<String>?): Jsr305State {
        var global: ReportLevel? = null
        var migration: ReportLevel? = null
        val userDefined = mutableMapOf<String, ReportLevel>()

        fun parseJsr305UnderMigration(item: String): ReportLevel? {
            val rawState = item.split(":").takeIf { it.size == 2 }?.get(1)
            return ReportLevel.findByDescription(rawState) ?: reportUnrecognizedJsr305(item).let { null }
        }

        args?.forEach { item ->
            when {
                item.startsWith("@") -> {
                    val (name, state) = parseJsr305UserDefined(item) ?: return@forEach
                    val current = userDefined[name]
                    if (current == null) {
                        userDefined[name] = state
                    } else if (current != state) {
                        reportDuplicateJsr305("@$name:${current.description}", item)
                        return@forEach
                    }
                }
                item.startsWith("under-migration") -> {
                    val state = parseJsr305UnderMigration(item)
                    if (migration == null) {
                        migration = state
                    } else if (migration != state) {
                        reportDuplicateJsr305("under-migration:${migration?.description}", item)
                        return@forEach
                    }
                }
                item == "enable" -> {
                    collector.report(
                        CompilerMessageSeverity.STRONG_WARNING,
                        "Option 'enable' for -Xjsr305 flag is deprecated. Please use 'strict' instead"
                    )
                    if (global != null) return@forEach

                    global = ReportLevel.STRICT
                }
                else -> {
                    if (global == null) {
                        global = ReportLevel.findByDescription(item)
                    } else if (global!!.description != item) {
                        reportDuplicateJsr305(global!!.description, item)
                        return@forEach
                    }
                }
            }
        }
        return Jsr305State(global, migration, userDefined)
    }

    private fun reportUnrecognizedJsr305(item: String) {
        collector.report(CompilerMessageSeverity.ERROR, "Unrecognized -Xjsr305 value: $item")
    }

    private fun reportDuplicateJsr305(first: String, second: String) {
        collector.report(CompilerMessageSeverity.ERROR, "Conflict duplicating -Xjsr305 value: $first, $second")
    }

    private fun parseJsr305UserDefined(item: String): Pair<String, ReportLevel>? {
        val (name, rawState) = item.substring(1).split(":").takeIf { it.size == 2 } ?: run {
            reportUnrecognizedJsr305(item)
            return null
        }

        val state = ReportLevel.findByDescription(rawState) ?: run {
            reportUnrecognizedJsr305(item)
            return null
        }

        return name to state
    }
}
