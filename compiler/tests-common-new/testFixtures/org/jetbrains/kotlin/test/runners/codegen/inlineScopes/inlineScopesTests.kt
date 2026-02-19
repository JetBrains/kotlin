/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen.inlineScopes

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.useInlineScopesNumbers
import org.jetbrains.kotlin.test.runners.codegen.*

/*
 * All tests in this file are Fir Light Tree tests because they are meant to test inline scopes numbers
 * in the JVM backend and their execution result shouldn't be affected by the parser.
 */

open class AbstractFirBlackBoxInlineCodegenTestWithInlineScopes :
    AbstractFirLightTreeBlackBoxInlineCodegenTest() {
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

open class AbstractFirBytecodeTextTestWithInlineScopes : AbstractFirLightTreeBytecodeTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirLocalVariableTestWithInlineScopes : AbstractFirLightTreeLocalVariableTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}

open class AbstractFirSteppingTestWithInlineScopes : AbstractFirLightTreeSteppingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useInlineScopesNumbers()
    }
}
