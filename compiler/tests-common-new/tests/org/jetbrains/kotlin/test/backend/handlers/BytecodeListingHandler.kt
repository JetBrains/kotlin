/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumperImpl
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension

class BytecodeListingHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumperImpl()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (CHECK_BYTECODE_LISTING !in module.directives) return
        val dump = BytecodeListingTextCollectingVisitor.getText(
            info.classFileFactory,
            BytecodeListingTextCollectingVisitor.Filter.ForCodegenTests
        )
        multiModuleInfoDumper.builderForModule(module).append(dump)

    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (multiModuleInfoDumper.isEmpty()) return
        val suffix = if (testServices.defaultsProvider.defaultTargetBackend?.isIR == true) "_ir" else ""
        val file = testServices.moduleStructure.originalTestDataFiles.first()
            .withSuffixAndExtension(suffix, ".txt")
        assertions.assertEqualsToFile(file, multiModuleInfoDumper.generateResultingDump())
    }
}
