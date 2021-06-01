/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.FqName

data class Jsr305Settings(
    val globalLevel: ReportLevel,
    val migrationLevel: ReportLevel? = null,
    val userDefinedLevelForSpecificAnnotation: Map<FqName, ReportLevel> = emptyMap()
) {
    companion object {
        val DEFAULT by lazy {
            val reportLevelBefore = if (jsr305Settings.sinceVersion != null && jsr305Settings.sinceVersion <= KotlinVersion.CURRENT) {
                jsr305Settings.reportLevelBefore
            } else jsr305Settings.reportLevelAfter

            Jsr305Settings(reportLevelBefore, if (reportLevelBefore == ReportLevel.WARN) null else reportLevelBefore)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    val description by lazy {
        buildList {
            add(globalLevel.description)
            migrationLevel?.let { add("under-migration:${it.description}") }
            userDefinedLevelForSpecificAnnotation.forEach { add("@${it.key}:${it.value.description}") }
        }.toTypedArray()
    }

    val isDisabled = globalLevel == ReportLevel.IGNORE
            && migrationLevel == ReportLevel.IGNORE
            && userDefinedLevelForSpecificAnnotation.isEmpty()
}