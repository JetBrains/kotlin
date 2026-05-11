/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.KtResolveExtensionTestSupport
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractResolveSymbolWithResolveExtensionTest : AbstractResolveSymbolTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        KtResolveExtensionTestSupport.configure(builder)
    }

    context(session: KaSession)
    override fun additionalSymbolInfo(attempt: KaSymbolResolutionAttempt): String? {
        return attempt.symbols.takeUnless { it.isEmpty() }?.joinToString { it.origin.toString() }
    }
}
