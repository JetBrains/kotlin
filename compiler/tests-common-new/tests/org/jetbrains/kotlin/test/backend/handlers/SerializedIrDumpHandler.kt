/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.defaultDumpIrTreeOptions
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

/**
 * This is a special IR dump handler required to test absence of losses during IR serialization/IR deserialization
 * cycle in KLIB-based backends. It dumps the IR text to a temporary file immediately before serializing IR to a KLIB,
 * and then dumps the deserialized IR text and checks it against the previously written file.
 *
 * The typical usage of this handler is the following:
 * ```
 * irHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) }) }
 * facadeStep(<serializer>)
 * klibArtifactsHandlersStep()
 * facadeStep(<deserializer>)
 * deserializedIrHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) }) }
 * ```
 *
 * @property isAfterDeserialization Whether the handler is to be executed before serialization (false) or after deserialization (true).
 */
class SerializedIrDumpHandler(
    testServices: TestServices,
    private val isAfterDeserialization: Boolean,
) : AbstractIrHandler(testServices) {

    private val dumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives) // TODO: extract and rename SKIP_DESERIALIZED_IR_TEXT_DUMP from CodegenTestDirectives

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (module.isSkipped) return

        val dumpOptions = defaultDumpIrTreeOptions(module, info.irPluginContext.irBuiltIns).copy(
            printFlagsInDeclarationReferences = true,
        )
        val builder = dumper.builderForModule(module.name)

        val irFiles = info.irModuleFragment.files.sortedBy { it.fileEntry.name }
        for (irFile in irFiles) {
            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            builder.append(actualDump)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (testServices.moduleStructure.modules.any { it.isSkipped }) return

        val dump = dumper.generateResultingDump()
        val dumpFile = testServices.dumpFile

        if (!isAfterDeserialization) {
            assertions.assertFileDoesntExist(dumpFile) { "Dump file already exists: $dumpFile" }
            dumpFile.writeText(PRE_SERIALIZATION_DUMP_SANITIZER.sanitize(dump))
        } else {
            assertions.assertEqualsToFile(dumpFile, POST_DESERIALIZATION_DUMP_SANITIZER.sanitize(dump))
        }
    }

    companion object {
        private val TestModule.isSkipped: Boolean
            // TODO: extract and rename SKIP_DESERIALIZED_IR_TEXT_DUMP from CodegenTestDirectives
            get() = CodegenTestDirectives.SKIP_DESERIALIZED_IR_TEXT_DUMP in directives

        private val TestServices.dumpFile: File
            get() = temporaryDirectoryManager.rootDir.resolve("ir_pre_serialization_dump.txt")

        private val PRE_SERIALIZATION_DUMP_SANITIZER = IrDumpSanitizer.composite(
            FilterOutSealedSubclasses,
        )

        private val POST_DESERIALIZATION_DUMP_SANITIZER = IrDumpSanitizer.composite(/* to be added */)
    }
}

private fun interface IrDumpSanitizer {
    fun sanitize(testData: String): String

    companion object {
        fun composite(vararg sanitizers: IrDumpSanitizer) = IrDumpSanitizer { testData ->
            sanitizers.fold(testData) { acc, sanitizer -> sanitizer.sanitize(acc) }
        }
    }
}

/**
 * Sealed subclasses are not deserialized from KLIBs, so we need to wipe them out from dumps.
 * For details see KT-54028,
 * and in particular the commit https://github.com/jetbrains/kotlin/commit/3713d95bb1fc0cc434eeed42a0f0adac52af091b
 */
private object FilterOutSealedSubclasses : IrDumpSanitizer {
    override fun sanitize(testData: String) = buildString {
        var ongoingSealedSubclassesClauseIndent: String? = null
        for (line in testData.lines()) {
            if (ongoingSealedSubclassesClauseIndent == null) {
                if (line.trim() == SEALED_SUBCLASSES_CLAUSE) {
                    ongoingSealedSubclassesClauseIndent = line.substringBefore(SEALED_SUBCLASSES_CLAUSE)
                } else {
                    appendLine(line)
                }
            } else {
                if (!line.startsWith("$ongoingSealedSubclassesClauseIndent  CLASS")) {
                    ongoingSealedSubclassesClauseIndent = null
                    appendLine(line)
                }
            }
        }
    }

    private const val SEALED_SUBCLASSES_CLAUSE = "sealedSubclasses:"
}
