/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSubtypeTrackerComponent
import org.jetbrains.kotlin.incremental.components.SubtypeTracker
import org.jetbrains.kotlin.name.ClassId

class IncrementalPassThroughSubtypeTrackerComponent(private val subtypeTracker: SubtypeTracker) : FirSubtypeTrackerComponent() {
    override fun report(supertype: ClassId, subtype: ClassId) {
        subtypeTracker.report(supertype.asSingleFqName(), subtype.asSingleFqName())
    }
}
