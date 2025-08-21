/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.cli

import org.jetbrains.kotlin.test.cli.CliDirectives.CHECK_COMPILER_OUTPUT
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.handlers.NonSourceErrorMessagesHandler
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper

@DeprecatedCliFacades
class CliOutputHandler(testServices: TestServices) : BinaryArtifactHandler<CliArtifact>(
    testServices,
    CliArtifact.Kind,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumper()
    private val delegateHandler = NonSourceErrorMessagesHandler(testServices)

    override fun processModule(module: TestModule, info: CliArtifact) {
        if (info.kotlinOutput.isEmpty()) return
        if (CHECK_COMPILER_OUTPUT !in module.directives) return
        multiModuleInfoDumper.builderForModule(module).append(info.kotlinOutput)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        delegateHandler.check(multiModuleInfoDumper.generateResultingDump())
    }
}
