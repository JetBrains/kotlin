/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface LLFirSessionConfigurator {
    companion object : ProjectExtensionDescriptor<LLFirSessionConfigurator>(
        "org.jetbrains.kotlin.llFirSessionConfigurator",
        LLFirSessionConfigurator::class.java
    )

    fun configure(session: LLFirSession)
}