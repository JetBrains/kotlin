/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen.inlineScopes

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.*

/*
 * All tests in this file are Fir Light Tree tests because they are meant to test inline scopes numbers
 * in the JVM backend and their execution result shouldn't be affected by the parser.
 */

open class AbstractFirBlackBoxInlineCodegenWithBytecodeInlinerTestWithInlineScopes :
    AbstractFirLightTreeBlackBoxInlineCodegenWithBytecodeInlinerTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirBlackBoxInlineCodegenWithIrInlinerTestWithInlineScopes : AbstractFirLightTreeBlackBoxInlineCodegenWithIrInlinerTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

// Adding this test will result in test failures.
// TODO: Decide what to do with this test
open class AbstractFirAsmLikeInstructionListingTestWithInlineScopes : AbstractFirLightTreeAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirBlackBoxCodegenTestWithInlineScopes : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

// Adding this test will result in test failures.
// TODO: Add or remove when the fate of the IR inliner is decided.
open class AbstractFirBlackBoxCodegenWithIrInlinerTestWithInlineScopes : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
        builder.useIrInliner()
    }
}

open class AbstractFirBytecodeTextTestWithInlineScopes : AbstractFirLightTreeBytecodeTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirLocalVariableBytecodeInlinerTestWithInlineScopes : AbstractFirLightTreeLocalVariableTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirLocalVariableIrInlinerTestWithInlineScopes : AbstractFirLightTreeLocalVariableTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
        builder.useIrInliner()
    }
}

open class AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestWithInlineScopes :
    AbstractFirLightTreeSerializeCompileKotlinAgainstInlineKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirSteppingWithBytecodeInlinerTestWithInlineScopes : AbstractFirLightTreeSteppingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirSteppingWithIrInlinerTestWithInlineScopes : AbstractFirLightTreeSteppingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
        builder.useIrInliner()
    }
}
