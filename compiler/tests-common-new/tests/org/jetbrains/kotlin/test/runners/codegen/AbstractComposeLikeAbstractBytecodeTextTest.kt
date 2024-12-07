/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.configuration.ComposeLikeConfigurator

abstract class AbstractComposeLikeFirAbstractBytecodeTextTestBase(parser: FirParser): AbstractFirBytecodeTextTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(::ComposeLikeConfigurator)
        }
    }
}

open class AbstractComposeLikeIrBytecodeTextTest(): AbstractIrBytecodeTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(::ComposeLikeConfigurator)
        }
    }
}


open class AbstractComposeLikeFirLightTreeBytecodeTextTest : AbstractComposeLikeFirAbstractBytecodeTextTestBase(FirParser.LightTree)
