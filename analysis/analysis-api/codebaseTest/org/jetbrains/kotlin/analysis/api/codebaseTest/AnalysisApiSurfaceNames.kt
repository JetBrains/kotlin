/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

internal object AnalysisApiSurfaceNames {
    val KA_SESSION: String = KaSession::class.simpleName!!

    /**
     * Fully qualified names of the session components: the interfaces mixed into [KaSession], except [KaLifetimeOwner].
     */
    val KA_SESSION_COMPONENTS: Set<String> =
        KaSession::class.java.interfaces.mapTo(mutableSetOf()) { it.name } - KaLifetimeOwner::class.java.name

    @OptIn(KaImplementationDetail::class)
    val KA_INTERNALS: String = KaInternals::class.simpleName!!

    const val KA_IMPLEMENTATION_DETAIL: String = "KaImplementationDetail"

    const val IMPLEMENTATION_DETAIL: String = "KaSessionComponentImplementationDetail"
    const val IMPLEMENTATION_DETAIL_ANNOTATION: String = "@$IMPLEMENTATION_DETAIL"
    val IMPLEMENTATION_DETAIL_SUBCLASS_ANNOTATION: String =
        "@${SubclassOptInRequired::class.simpleName}($IMPLEMENTATION_DETAIL::class)"
}
