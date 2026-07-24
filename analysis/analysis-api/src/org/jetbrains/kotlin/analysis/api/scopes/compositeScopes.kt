/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals

/**
 * Combines a list of [KaScope]s into a single composite [KaScope]. The resulting scope contains all members of its constituent scopes.
 */
context(session: KaSession)
public fun List<KaScope>.asCompositeScope(): KaScope {
    @OptIn(KaImplementationDetail::class)
    return internals.scopeProvider.asCompositeScope(this)
}
