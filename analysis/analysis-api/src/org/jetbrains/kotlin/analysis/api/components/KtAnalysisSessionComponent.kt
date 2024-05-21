/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken

public abstract class KaSessionComponent {
    protected abstract val analysisSession: KaSession
    protected open val token: KaLifetimeToken get() = analysisSession.token
}

public typealias KtAnalysisSessionComponent = KaSessionComponent