/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.KtResolveExtensionTestSupport
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractResolveReferenceWithResolveExtensionTest : AbstractResolveReferenceTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        KtResolveExtensionTestSupport.configure(builder)
    }

    override fun KaSession.getAdditionalSymbolInfo(symbol: KaSymbol): String? =
        "origin = ${symbol.origin}"
}
