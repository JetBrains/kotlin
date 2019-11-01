/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractFirDiagnosticsWithStdlibTest : AbstractFirDiagnosticsTest() {
    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun getTestJdkKind(file: File): TestJdkKind {
        return TestJdkKind.FULL_JDK
    }
}