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
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf

class JavaTypeEnhancementStateParser(private val collector: MessageCollector) {
    fun parse(
        jsr305Args: Array<String>?,
        supportCompatqualCheckerFrameworkAnnotations: String?,
        jspecifyState: String?,
        nullabilityAnnotations: Array<String>?,
    ): JavaTypeEnhancementState {
        val compatqualCheckerFrameworkAnnotationsReportLevel = when (supportCompatqualCheckerFrameworkAnnotations) {
            "enable" -> ReportLevel.STRICT
            "disable" -> ReportLevel.IGNORE
            null -> getDefaultReportLevelForAnnotation(CHECKER_FRAMEWORK_COMPATQUAL_ANNOTATIONS_PACKAGE)
            else -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unrecognized -Xsupport-compatqual-checker-framework-annotations option: $supportCompatqualCheckerFrameworkAnnotations. Possible values are 'enable'/'disable'"
                )
                getDefaultReportLevelForAnnotation(CHECKER_FRAMEWORK_COMPATQUAL_ANNOTATIONS_PACKAGE)
            }
        }
        val jsr305Settings = parseJsr305State(jsr305Args)
        val jspecifyReportLevel = parseJspecifyReportLevel(jspecifyState)
        val nullabilityAnnotationReportLevels = parseNullabilityAnnotationReportLevels(nullabilityAnnotations)

        return JavaTypeEnhancementState(jsr305Settings) {
            when {
                it.isSubpackageOf(JSPECIFY_ANNOTATIONS_PACKAGE) -> jspecifyReportLevel
                it.isSubpackageOf(CHECKER_FRAMEWORK_COMPATQUAL_ANNOTATIONS_PACKAGE) -> compatqualCheckerFrameworkAnnotationsReportLevel
                else -> getReportLevelForAnnotation(it, nullabilityAnnotationReportLevels)
            }
        }
    }

    private fun parseNullabilityAnnotationReportLevels(nullabilityAnnotations: Array<String>?): Map<FqName, ReportLevel> {
        if (nullabilityAnnotations.isNullOrEmpty()) return emptyMap()

        val annotationsWithReportLevels = mutableMapOf<FqName, ReportLevel>()
        val compilerOption = "-Xnullability-annotations"

        for (item in nullabilityAnnotations) {
            if (!item.startsWith("@")) {
                reportUnrecognizedReportLevel(item, compilerOption)
                continue
            }

            val (name, state) = parseAnnotationWithReportLevel(item, compilerOption) ?: continue
            val current = annotationsWithReportLevels[name]
            if (current == null) {
                annotationsWithReportLevels[name] = state
            } else if (current != state) {
                reportDuplicateAnnotation("@$name:${current.description}", item, compilerOption)
                continue
            }
        }

        return annotationsWithReportLevels
    }

    private fun parseJspecifyReportLevel(jspecifyState: String?): ReportLevel {
        if (jspecifyState == null)
            return getDefaultReportLevelForAnnotation(JSPECIFY_ANNOTATIONS_PACKAGE)

        val reportLevel = ReportLevel.findByDescription(jspecifyState)

        if (reportLevel == null) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Unrecognized -Xjspecify-annotations option: $jspecifyState. Possible values are 'disable'/'warn'/'strict'"
            )
            return getDefaultReportLevelForAnnotation(JSPECIFY_ANNOTATIONS_PACKAGE)
        }

        return reportLevel
    }

    private fun parseJsr305State(args: Array<String>?): Jsr305Settings {
        var global: ReportLevel? = null
        var migration: ReportLevel? = null
        val userDefined = mutableMapOf<FqName, ReportLevel>()
        val compilerOption = "-Xjsr305"
        val defaultSettings = Jsr305Settings.DEFAULT

        fun parseJsr305UnderMigration(item: String): ReportLevel? {
            val rawState = item.split(":").takeIf { it.size == 2 }?.get(1)
            return ReportLevel.findByDescription(rawState) ?: reportUnrecognizedReportLevel(item, compilerOption).let { null }
        }

        args?.forEach { item ->
            when {
                item.startsWith("@") -> {
                    val (name, state) = parseAnnotationWithReportLevel(item, compilerOption) ?: return@forEach
                    val current = userDefined[name]
                    if (current == null) {
                        userDefined[name] = state
                    } else if (current != state) {
                        reportDuplicateAnnotation("@$name:${current.description}", item, compilerOption)
                        return@forEach
                    }
                }
                item.startsWith("under-migration") -> {
                    val state = parseJsr305UnderMigration(item)
                    if (migration == null) {
                        migration = state
                    } else if (migration != state) {
                        reportDuplicateAnnotation("under-migration:${migration?.description}", item, compilerOption)
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
                        reportDuplicateAnnotation(global!!.description, item, compilerOption)
                        return@forEach
                    }
                }
            }
        }

        val globalLevel = global ?: defaultSettings.globalLevel

        return Jsr305Settings(
            globalLevel = globalLevel,
            migrationLevel = migration ?: getDefaultMigrationJsr305ReportLevelForGivenGlobal(globalLevel),
            userDefinedLevelForSpecificAnnotation = defaultSettings.userDefinedLevelForSpecificAnnotation
        )
    }

    private fun reportUnrecognizedReportLevel(item: String, sourceCompilerOption: String) {
        collector.report(CompilerMessageSeverity.ERROR, "Unrecognized $sourceCompilerOption value: $item")
    }

    private fun reportDuplicateAnnotation(first: String, second: String, sourceCompilerOption: String) {
        collector.report(CompilerMessageSeverity.ERROR, "Conflict duplicating $sourceCompilerOption value: $first, $second")
    }

    private fun parseAnnotationWithReportLevel(item: String, sourceCompilerOption: String): Pair<FqName, ReportLevel>? {
        val (name, rawState) = item.substring(1).split(":").takeIf { it.size == 2 } ?: run {
            reportUnrecognizedReportLevel(item, sourceCompilerOption)
            return null
        }

        val state = ReportLevel.findByDescription(rawState) ?: run {
            reportUnrecognizedReportLevel(item, sourceCompilerOption)
            return null
        }

        return FqName(name) to state
    }

    companion object {
        private val DEFAULT = JavaTypeEnhancementStateParser(MessageCollector.NONE)

        fun parsePlainNullabilityAnnotationReportLevels(nullabilityAnnotations: String) =
            DEFAULT.parseNullabilityAnnotationReportLevels(arrayOf(nullabilityAnnotations)).entries.singleOrNull()?.toPair()
    }
}
