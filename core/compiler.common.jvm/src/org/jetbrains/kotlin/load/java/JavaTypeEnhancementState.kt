/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.FqName

class JavaTypeEnhancementState(
    val jsr305: Jsr305Settings,
    val getReportLevelForAnnotation: (FqName) -> ReportLevel
) {
    val disabledDefaultAnnotations = jsr305.isDisabled || getReportLevelForAnnotation(JSPECIFY_ANNOTATIONS_PACKAGE) == ReportLevel.IGNORE

    companion object {
        val DEFAULT = JavaTypeEnhancementState(getDefaultJsr305Settings(), ::getDefaultReportLevelForAnnotation)
    }

    override fun toString(): String {
        return "JavaTypeEnhancementState(jsr305=$jsr305, getReportLevelForAnnotation=$getReportLevelForAnnotation)"
    }
}
