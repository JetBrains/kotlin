/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.CommonSMAPTestUtil
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.codegen.inline.GENERATE_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.NO_SMAP_DUMP
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

class SMAPDumpHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    companion object {
        const val SMAP_EXT = "smap"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    private val dumper = MultiModuleInfoDumper(moduleHeaderTemplate = null)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        checkArtifact(info)
        if (!GENERATE_SMAP) return
        if (DUMP_SMAP !in module.directives) return

        val originalFileNames = module.files.map { it.name }

        val compiledSmaps = CommonSMAPTestUtil.extractSMAPFromClasses(info.classFileFactory.getClassFiles()).mapNotNull {
            val name = File(it.sourceFile).name
            val index = originalFileNames.indexOf(name)
            val testFile = module.files[index]
            if (NO_SMAP_DUMP in testFile.directives) return@mapNotNull null
            index to it
        }.sortedBy { it.first }.map { it.second }

        CommonSMAPTestUtil.checkNoConflictMappings(compiledSmaps, assertions)

        val compiledData = compiledSmaps.groupBy {
            it.sourceFile
        }.map {
            val smap = it.value.sortedByDescending(CommonSMAPTestUtil.SMAPAndFile::outputFile).mapNotNull(CommonSMAPTestUtil.SMAPAndFile::smap).joinToString("\n")
            CommonSMAPTestUtil.SMAPAndFile(if (smap.isNotEmpty()) smap else null, it.key, "NOT_SORTED")
        }.associateBy { it.sourceFile }

        dumper.builderForModule(module).apply {
            for (source in compiledData.values) {
                appendLine("// FILE: ${File(source.sourceFile).name}")
                appendLine(source.smap ?: "")
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val actualDump = if (dumper.isEmpty()) null else dumper.generateResultingDump()
        assertEqualsToDump(SMAP_EXT, actualDump)
    }
}
