/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives

open class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractIrBlackBoxInlineCodegenTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractFirBlackBoxInlineCodegenTest : AbstractFirBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractFirLightTreeBlackBoxInlineCodegenTest : AbstractFirBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useInlineHandlers()
            defaultDirectives {
                +FirDiagnosticsDirectives.USE_LIGHT_TREE
            }
        }
    }
}
