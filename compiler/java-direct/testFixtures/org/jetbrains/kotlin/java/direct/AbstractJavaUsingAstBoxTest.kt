/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase

abstract class AbstractJavaUsingAstBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            useConfigurators(
                ::JavaDirectConfigurator
            )
        }
        super.configure(builder)
    }
}
