/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.WITH_SIGNATURES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import java.io.File

class BytecodeListingHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (CHECK_BYTECODE_LISTING !in module.directives) return
        val dump = BytecodeListingTextCollectingVisitor.getText(
            info.classFileFactory,
            BytecodeListingTextCollectingVisitor.Filter.ForCodegenTests,
            withSignatures = WITH_SIGNATURES in module.directives,
            withAnnotations = IGNORE_ANNOTATIONS !in module.directives,
        )
        multiModuleInfoDumper.builderForModule(module).append(dump)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (multiModuleInfoDumper.isEmpty()) return

        val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
        val extension =
            if (testServices.defaultsProvider.defaultFrontend == FrontendKinds.FIR && FIR_IDENTICAL !in testServices.moduleStructure.allDirectives) ".fir.txt" else ".txt"
        val defaultTxtFile = sourceFile.withExtension(extension)
        val isIr = testServices.defaultsProvider.defaultTargetBackend?.isIR == true
        val txtFile =
            if (isIr) sourceFile.withSuffixAndExtension("_ir", extension).takeIf(File::exists) ?: defaultTxtFile
            else defaultTxtFile

        assertions.assertEqualsToFile(txtFile, multiModuleInfoDumper.generateResultingDump())

        if (isIr && txtFile != defaultTxtFile) {
            if (txtFile.readText() == defaultTxtFile.readText()) assertions.fail {
                "JVM and JVM_IR golden files are identical. Remove $txtFile."
            }
        }
    }
}
