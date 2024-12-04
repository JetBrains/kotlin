/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken

@KaImplementationDetail
abstract class KaSessionComponent<T : KaSession> : KaLifetimeOwner {
    abstract val analysisSessionProvider: () -> T

    val analysisSession: T
        get() = analysisSessionProvider()

    final override val token: KaLifetimeToken
        get() = analysisSession.token
}
