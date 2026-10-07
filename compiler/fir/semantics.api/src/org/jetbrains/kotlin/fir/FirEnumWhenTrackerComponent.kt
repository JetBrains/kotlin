/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId

abstract class FirEnumWhenTrackerComponent : FirSessionComponent {
    abstract fun report(whenExpressionFilePath: String, enumClassFqName: String)
}

val FirSession.enumWhenTracker: FirEnumWhenTrackerComponent? by FirSession.nullableSessionComponentAccessor()

fun FirEnumWhenTrackerComponent.reportEnumUsageInWhen(path: String?, subjectType: ConeKotlinType?) {
    if (path == null || subjectType == null) return
    val fqName = subjectType.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
    this.report(path, fqName)
}