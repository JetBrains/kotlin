/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.initIdeaConfiguration
import org.jetbrains.kotlin.test.services.KotlinTestInfo

object K1K2UIComparatorApi {
    private val emptyKotlinTestInfo = KotlinTestInfo(
        className = "_undefined_",
        methodName = "_testUndefined_",
        tags = emptySet()
    )

    fun compileSourceAndDecorateWithDiagnostics(
        source: String,
        runner: AbstractKotlinCompilerTest,
        pathForConfigurationImitation: String = "",
    ): String {
        initIdeaConfiguration()
        runner.initTestInfo(emptyKotlinTestInfo)
        return testRunner(pathForConfigurationImitation, runner.configuration).runTestSource(source)
    }
}
