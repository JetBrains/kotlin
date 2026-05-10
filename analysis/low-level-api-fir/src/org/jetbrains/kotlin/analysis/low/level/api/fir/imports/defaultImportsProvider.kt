/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.imports

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLPlatformSessionComponentRegistration
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

@KaImplementationDetail
val TargetPlatform.defaultImportsProvider: DefaultImportsProvider
    get() {
        val defaultImportsProviders = LLPlatformSessionComponentRegistration
            .forPlatform(this)
            .map { it.defaultImportsProvider }

        return when (defaultImportsProviders.size) {
            0 -> error("No default imports provider found for platform '$this'")
            1 -> defaultImportsProviders.single()
            else -> DefaultImportsProvider.Composed(defaultImportsProviders)
        }
    }
