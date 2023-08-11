/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirImportTrackerComponent
import org.jetbrains.kotlin.incremental.components.ImportTracker


class IncrementalPassThroughImportTrackerComponent(private val importTracker: ImportTracker) : FirImportTrackerComponent() {
    override fun report(filePath: String, importedFqName: String) {
        importTracker.report(filePath, importedFqName)
    }
}