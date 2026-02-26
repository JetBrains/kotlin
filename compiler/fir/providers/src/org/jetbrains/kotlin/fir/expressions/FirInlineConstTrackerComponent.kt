/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

open class FirInlineConstTrackerComponent(val inlineConstTracker: InlineConstTracker?) : FirSessionComponent {
    object Default : FirInlineConstTrackerComponent(null)
}

val FirSession.inlineConstTracker: FirInlineConstTrackerComponent by FirSession.sessionComponentAccessor<FirInlineConstTrackerComponent>()
