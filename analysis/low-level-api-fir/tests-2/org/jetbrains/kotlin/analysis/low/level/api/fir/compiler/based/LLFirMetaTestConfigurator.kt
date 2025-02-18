/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.llFirTestDataFile
import java.io.File

/**
 * Uses `.ll.kt` test data if available.
 */
class LLFirMetaTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val llFirFile = File(testDataFileName).llFirTestDataFile
        return if (llFirFile.exists()) llFirFile.path else testDataFileName
    }
}
