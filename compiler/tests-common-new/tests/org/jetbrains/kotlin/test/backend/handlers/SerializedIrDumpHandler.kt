/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.declarations.IrPossiblyExternalDeclaration
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler.Companion.defaultDumpIrTreeOptions
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_IR_DESERIALIZATION_CHECKS
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
        get() = listOf(KlibBasedCompilerTestDirectives)

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
            get() = SKIP_IR_DESERIALIZATION_CHECKS in directives

        private val TestServices.dumpFile: File
            get() = temporaryDirectoryManager.rootDir.resolve("ir_pre_serialization_dump.txt")

        private val PRE_SERIALIZATION_DUMP_SANITIZER = IrDumpSanitizer.composite(
            FilterOutSealedSubclasses,
            RemoveExternalFlagInRenderedSymbolOfIrDeclarationReference,
        )

        private val POST_DESERIALIZATION_DUMP_SANITIZER = IrDumpSanitizer.composite(
            RemoveExternalFlagInRenderedSymbolOfIrDeclarationReference,
        )
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

/**
 * The text of every rendered [IrDeclarationReference] expression contains some details about the referenced symbol and
 * its "owner" declaration. As a part of this text, there are declaration flags, one of which can be the "external" flag.
 * This flag is rendered if the declaration is marked as "external" (see [IrPossiblyExternalDeclaration.isExternal]).
 *
 * However, there is a difference in how K1 and K2 compilers propagate "external"-ness to declarations in metadata, and
 * how such declarations are later represented in the form of LazyIr and Fir2LazyIr. This makes it difficult to compare
 * this flag 1-to-1 in the IR that was before serialization and the deserialized IR.
 *
 * We know that there are two strict rules that allow correctly handling "external" flag in the compiler:
 * 1.Language permits `external` keyword only for top-level declarations. So, any top-level declaration must have this flag
 *   written in metadata and subsequently in IR proto.
 * 2.Whenever the compiler needs to check "external"-ness of a declaration, it should check the effective "external"-ness
 *   (i.e., taking into account containing declarations, if any).
 *
 * Given this, it makes sense to filter out any mentioning of this flag in IR expressions that may refer to symbols
 * bound to LazyIr/Fir2LazyIr declarations.
 */
private object RemoveExternalFlagInRenderedSymbolOfIrDeclarationReference : IrDumpSanitizer {
    override fun sanitize(testData: String) = testData.lineSequence().map { line ->
        line.replace(IR_DECLARATION_REFERENCE_WITH_RENDERED_SYMBOL_REGEX) { match ->
            val groupWithFlags = match.groups[2]!!

            val prefix = line.substring(0, groupWithFlags.range.start)
            val suffix = line.substring(groupWithFlags.range.endInclusive + 1)

            val filteredFlags = groupWithFlags.value
                .removeSurrounding("[", "] ")
                .split(",")
                .filter { it != "external" }
                .takeIf(List<*>::isNotEmpty)
                ?.joinToString(",", prefix = "[", postfix = "] ")
                .orEmpty()

            prefix + filteredFlags + suffix
        }
    }.joinToString("\n")

    private val IR_DECLARATION_REFERENCE_PREFIXES = listOf(
        "CALL",                         // IrCall
        "CONSTRUCTOR_CALL",             // IrConstructorCall
        "DELEGATING_CONSTRUCTOR_CALL",  // IrDelegatingConstructorCall
        "PROPERTY_REFERENCE",           // IrPropertyReference
        "FUNCTION_REFERENCE",           // IrFunctionReference
        "CLASS_REFERENCE",              // IrClassReference
        "GET_OBJECT",                   // IrGetObjectValue
    ).joinToString("|", prefix = "(", postfix = ")")

    private val IR_DECLARATION_REFERENCE_WITH_RENDERED_SYMBOL_REGEX = Regex(".* $IR_DECLARATION_REFERENCE_PREFIXES .*(\\[[a-z,]+] ).*")
}
