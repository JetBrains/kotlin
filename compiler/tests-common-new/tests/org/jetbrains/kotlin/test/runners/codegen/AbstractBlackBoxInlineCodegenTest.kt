/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

open class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractIrBlackBoxInlineCodegenWithBytecodeInlinerTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractIrBlackBoxInlineCodegenWithIrInlinerTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
        builder.useIrInliner()
    }
}

open class AbstractFirLightTreeBlackBoxInlineCodegenWithBytecodeInlinerTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

open class AbstractFirLightTreeBlackBoxInlineCodegenWithIrInlinerTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
        builder.useIrInliner()
    }
}

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxInlineCodegenWithBytecodeInlinerTest : AbstractFirPsiBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
    }
}

@FirPsiCodegenTest
open class AbstractFirPsiBlackBoxInlineCodegenWithIrInlinerTest : AbstractFirPsiBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineHandlers()
        builder.useIrInliner()
    }
}

