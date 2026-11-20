/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.JvmCodegenDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class JvmMainFunctionForBlackBoxTestsSourceProvider(testServices: TestServices) : MainFunctionForBlackBoxTestsSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> {
        if (REQUIRES_SEPARATE_PROCESS !in module.directives && module.directives.singleOrZeroValue(JDK_KIND)?.requiresSeparateProcess != true) {
            return emptyList()
        }
        return super.produceAdditionalFiles(globalDirectives, module, testModuleStructure)
    }
}
