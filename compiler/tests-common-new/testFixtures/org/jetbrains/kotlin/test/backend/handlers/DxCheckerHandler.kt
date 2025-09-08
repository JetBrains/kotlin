/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.D8Checker
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_DEXING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

class DxCheckerHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (RUN_DEX_CHECKER !in module.directives || IGNORE_DEXING in module.directives) return
        try {
            D8Checker.check(info.classFileFactory)
        } catch (e: Throwable) {
            if (!testServices.assertions.isTeamCityBuild &&
                !testServices.codegenSuppressionChecker.failuresInModuleAreIgnored(module)
            ) {
                try {
                    println(info.classFileFactory.createText())
                } catch (e1: Throwable) {
                    System.err.println("Exception thrown while trying to generate text:")
                    e1.printStackTrace()
                    System.err.println("---")
                }
            }
            throw e
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
