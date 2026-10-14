/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

abstract class FirImportTrackerComponent : FirSessionComponent {
    abstract fun report(filePath: String, importedFqName: String)
}

val FirSession.importTracker: FirImportTrackerComponent? by FirSession.nullableSessionComponentAccessor()

fun FirImportTrackerComponent.reportImportDirectives(filePath: String?, importedFqName: String?) {
    if (filePath == null || importedFqName == null) return
    this.report(filePath, importedFqName)
}