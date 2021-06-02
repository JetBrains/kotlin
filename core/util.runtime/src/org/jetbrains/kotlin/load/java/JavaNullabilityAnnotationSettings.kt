/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.findValueForMostSpecificFqname

val JSPECIFY_ANNOTATIONS_PACKAGE = FqName("org.jspecify.nullness")
val CHECKER_FRAMEWORK_COMPATQUAL_ANNOTATIONS_PACKAGE = FqName("org.checkerframework.checker.nullness.compatqual")

val nullabilityAnnotationSettings = mapOf(
    FqName("org.jetbrains.annotations") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("androidx.annotation") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("android.support.annotation") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("android.annotation") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("com.android.annotations") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("org.eclipse.jdt.annotation") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("org.checkerframework.checker.nullness.qual") to JavaNullabilityAnnotationsStatus.DEFAULT,
    CHECKER_FRAMEWORK_COMPATQUAL_ANNOTATIONS_PACKAGE to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("javax.annotation") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("edu.umd.cs.findbugs.annotations") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("io.reactivex.annotations") to JavaNullabilityAnnotationsStatus.DEFAULT,
    FqName("lombok") to JavaNullabilityAnnotationsStatus.DEFAULT,
    JSPECIFY_ANNOTATIONS_PACKAGE to JavaNullabilityAnnotationsStatus(
        reportLevelBefore = ReportLevel.WARN,
        sinceVersion = KotlinVersion(1, 6),
        reportLevelAfter = ReportLevel.STRICT
    ),
)

val jsr305Settings = JavaNullabilityAnnotationsStatus(
    reportLevelBefore = ReportLevel.WARN,
    sinceVersion = null
)

fun getDefaultReportLevelForAnnotation(annotationFqName: FqName) = getReportLevelForAnnotation(annotationFqName, emptyMap())

fun getReportLevelForAnnotation(annotation: FqName, configuredReportLevels: Map<FqName, ReportLevel>): ReportLevel {
    annotation.findValueForMostSpecificFqname(configuredReportLevels)?.let { return it }

    val defaultReportLevel = annotation.findValueForMostSpecificFqname(nullabilityAnnotationSettings) ?: return ReportLevel.IGNORE

    return if (defaultReportLevel.sinceVersion != null && defaultReportLevel.sinceVersion <= KotlinVersion.CURRENT) {
        defaultReportLevel.reportLevelAfter
    } else {
        defaultReportLevel.reportLevelBefore
    }
}


