/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

data class JavaNullabilityAnnotationsStatus(
    val reportLevelBefore: ReportLevel,
    val sinceVersion: KotlinVersion? = KotlinVersion(1, 0),
    val reportLevelAfter: ReportLevel = reportLevelBefore,
) {
    companion object {
        val DEFAULT = JavaNullabilityAnnotationsStatus(ReportLevel.STRICT)
    }
}
