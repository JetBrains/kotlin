/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DONT_SORT_DECLARATIONS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.WITH_SIGNATURES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class BytecodeListingHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumper()

    private var irDumpEnabled = false
    private var firDumpEnabled = false

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        irDumpEnabled = irDumpEnabled || DUMP_IR in module.directives
        firDumpEnabled = firDumpEnabled || FIR_DUMP in module.directives
        if (CHECK_BYTECODE_LISTING !in module.directives) return

        val classes = info.classFileFactory.getClassFiles()
            .sortedBy { it.relativePath }
            .map {
                ClassNode(Opcodes.API_VERSION).also { node ->
                    ClassReader(it.asByteArray()).accept(node, ClassReader.SKIP_CODE)
                }
            }

        val filter = BytecodeListingTextCollectingVisitor.Filter.ForCodegenTests
        val dump = classes.mapNotNull { node ->
            val visitor = BytecodeListingTextCollectingVisitor(
                filter,
                withSignatures = WITH_SIGNATURES in module.directives,
                withAnnotations = IGNORE_ANNOTATIONS !in module.directives,
                sortDeclarations = DONT_SORT_DECLARATIONS !in module.directives,
            )
            node.accept(visitor)

            if (!filter.shouldWriteClass(node.name)) null else visitor.text
        }.joinToString("\n\n", postfix = "\n")

        multiModuleInfoDumper.builderForModule(module).append(dump)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
        val defaultTxtFile = sourceFile.withExtension(".txt")
        val irTxtFile = sourceFile.withExtension(".ir.txt")
        val firTxtFile = sourceFile.withExtension(".fir.txt")

        val isFir = testServices.defaultsProvider.frontendKind == FrontendKinds.FIR
        val isIr = testServices.defaultsProvider.targetBackend?.isIR == true

        val actualFile = when {
            isFir -> firTxtFile.takeIf { it.exists() } ?: irTxtFile.takeIf { it.exists() } ?: defaultTxtFile
            isIr -> irTxtFile.takeIf { it.exists() } ?: defaultTxtFile
            else -> defaultTxtFile
        }

        val goldenFile = when {
            isFir -> irTxtFile.takeIf { it.exists() } ?: defaultTxtFile
            else -> defaultTxtFile
        }

        if (multiModuleInfoDumper.isEmpty()) {
            if (!irDumpEnabled && actualFile == irTxtFile ||
                !firDumpEnabled && actualFile == firTxtFile ||
                actualFile == defaultTxtFile
            ) {
                assertions.assertFileDoesntExist(actualFile, CHECK_BYTECODE_LISTING)
            }
            return
        }

        assertions.assertEqualsToFile(actualFile, multiModuleInfoDumper.generateResultingDump())

        if (actualFile != goldenFile) {
            if (actualFile.readText().trim() == goldenFile.readText().trim()) assertions.fail {
                "JVM and JVM_IR golden files are identical. Remove $actualFile."
            }
        }
    }
}
