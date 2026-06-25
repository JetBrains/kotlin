/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * The [KaModule] from whose perspective the analysis is performed. The use-site module defines the resolution scope of the [KaSession],
 * which signifies *where* symbols are located (such as sources, dependencies, and so on) and *which* symbols can be found in the first
 * place.
 */
context(session: KaSession)
public val useSiteModule: KaModule
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.useSiteModule
    }

/**
 * The [KaSession] of the current analysis context.
 */
context(session: KaSession)
public val useSiteSession: KaSession
    get() = session
