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
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.WITH_SIGNATURES
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class BytecodeListingHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, CodegenTestDirectives)

    private val multiModuleInfoDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        checkArtifact(info)
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
        val actualDump = if (multiModuleInfoDumper.isEmpty()) null else multiModuleInfoDumper.generateResultingDump()
        assertEqualsToDump(extension = ".txt", actualDump)
    }
}
