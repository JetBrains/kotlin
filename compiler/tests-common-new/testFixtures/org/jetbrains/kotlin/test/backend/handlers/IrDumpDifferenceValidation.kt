/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File

/**
 * Validates target-specific IR dump files against the DUMP_IR_DIFFERENCE directive.
 *
 * - If there is no DUMP_IR_DIFFERENCE directive for the current backend, asserts that no
 *   target-specific dump file exists.
 * - If there is a directive but the target-specific dump is identical to the main dump,
 *   deletes the difference file and fails with a message asking to remove the backend
 *   from the directive.
 *
 * @param testServices the test services instance
 * @param mainExpectedFile the main (non-target-specific) expected dump file
 * @param baseDumpExtension the base dump extension without target override (e.g., "ir.txt" or "kt.txt")
 * @param actualDump the actual dump
 */
internal fun validateTargetSpecificDumpFile(
    testServices: TestServices,
    assertions: Assertions,
    mainExpectedFile: File,
    baseDumpExtension: String,
    actualDump: String,
    isKotlinLikeDump: Boolean,
): Boolean {
    val targetBackend = testServices.defaultsProvider.targetBackend ?: return false
    val targetBackendDirectiveName = targetBackend.name
    val moduleStructure = testServices.moduleStructure
    val dumpDescription = if (isKotlinLikeDump) "Kotlin-like IR dump" else "IR dump"

    val matchedBackend = testServices.getMatchedBackendFromDirective(CodegenTestDirectives.DUMP_IR_DIFFERENCE)
    if (matchedBackend != null) {
        val targetSpecificExtension = targetSpecificDumpExtension(baseDumpExtension, matchedBackend)
        val patchBackendName = targetBackend.directChildOf(matchedBackend).name.lowercase()
        val normalizedActualDump = actualDump.trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
        val targetSpecificFile = moduleStructure.originalTestDataFiles.first()
            .withExtension(targetSpecificExtension)

        if (normalizedActualDump.isEmpty()) {
            checkTestInfrastructure(!targetSpecificFile.exists()) {
                "DUMP_IR_DIFFERENCE directive specifies $targetBackendDirectiveName but there is no actual dump"
            }
            return true
        }

        checkTestInfrastructure(mainExpectedFile.exists()) {
            "DUMP_IR_DIFFERENCE directive specifies $targetBackendDirectiveName but neither main dump nor target-specific dump exists"
        }

        val mainDump = mainExpectedFile.readText().trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
        val expectedPatch = buildPatch(
            baseText = mainDump,
            targetText = normalizedActualDump,
            targetBackendName = patchBackendName,
            mainFileName = mainExpectedFile.name,
        )
        if (expectedPatch.isEmpty()) {
            if (targetSpecificFile.exists()) {
                checkTestInfrastructure(targetSpecificFile.delete()) {
                    "Unable to remove redundant target-specific $dumpDescription file: ${targetSpecificFile.absolutePath}"
                }
            }
            if (isKotlinLikeDump) {
                // Kotlin-like dumps show symbol's simple names, while text IR dumps show fqnames.
                // When a used symbol's package is changed -> `*ir.<backend>.patch` is not empty, while `*kt.<backend>.patch` may legitimately be empty.
                return true
            }
            assertions.fail {
                "There are no $dumpDescription differences. Please remove $targetBackendDirectiveName from DUMP_IR_DIFFERENCE directive"
            }
        }

        assertions.assertEqualsToFile(targetSpecificFile, expectedPatch)
        // Sanity check: patch application must result in the actual dump
        checkTestInfrastructure(applyPatch(mainDump, targetSpecificFile) == normalizedActualDump) {
            "Unable to reconstruct target-specific dump from patch: ${targetSpecificFile.absolutePath}"
        }
        return true
    } else {
        val baseTestFile = moduleStructure.originalTestDataFiles.first()

        val existingTargetSpecificFile = findTargetSpecificPatchFile(targetBackend, baseTestFile, baseDumpExtension)
        checkTestInfrastructure(existingTargetSpecificFile == null) {
            "Target-specific $dumpDescription file detected but no DUMP_IR_DIFFERENCE directive specified for " +
                    "$targetBackendDirectiveName or its compatible target: $existingTargetSpecificFile"
        }
        return false
    }
}

private fun findTargetSpecificPatchFile(targetBackend: TargetBackend, baseTestFile: File, baseDumpExtension: String): File? {
    var current = targetBackend
    while (current != TargetBackend.ANY) {
        val ext = targetSpecificDumpExtension(baseDumpExtension, current)
        val file = baseTestFile.withExtension(ext)
        if (file.exists()) {
            return file
        }
        current = current.compatibleWith
    }
    return null
}

private const val UNIFIED_CONTEXT_LINES = 3
private const val ORIGINAL_FILE_LABEL_PREFIX = "a/"
private const val UPDATED_FILE_LABEL_PREFIX = "b/"

private fun buildPatch(baseText: String, targetText: String, targetBackendName: String, mainFileName: String): String {
    val baseLines = baseText.lines()
    val targetLines = targetText.lines()
    if (baseLines == targetLines) return ""

    val patch = DiffUtils.diff(baseLines, targetLines)
    val updatedFileName = mainFileName.insertBackendBeforeTxtExtension(targetBackendName)
    val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
        "$ORIGINAL_FILE_LABEL_PREFIX$mainFileName",
        "$UPDATED_FILE_LABEL_PREFIX$updatedFileName",
        baseLines,
        patch,
        UNIFIED_CONTEXT_LINES,
    )
    return unifiedDiff
        .joinToString(System.lineSeparator())
        .trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
}

private fun String.insertBackendBeforeTxtExtension(targetBackendName: String): String {
    val txtSuffix = ".txt"
    return if (endsWith(txtSuffix)) {
        removeSuffix(txtSuffix) + "." + targetBackendName + txtSuffix
    } else {
        "$this.$targetBackendName"
    }
}

private fun applyPatch(baseText: String, patchFile: File): String {
    val patchText = patchFile.readText()
    val lines = patchText.lines().dropLastWhile { it.isEmpty() }
    checkTestInfrastructure(lines.size >= 3) {
        "Unknown target-specific patch format: ${patchFile.absolutePath}"
    }
    checkTestInfrastructure(lines[0].startsWith("--- ") && lines[1].startsWith("+++ ")) {
        "Unknown target-specific patch format: ${patchFile.absolutePath}"
    }

    val patchedLines = try {
        val patch = UnifiedDiffUtils.parseUnifiedDiff(lines)
        DiffUtils.patch(baseText.lines(), patch)
    } catch (e: Throwable) {
        testInfraError("Unknown target-specific patch format in ${patchFile.absolutePath}: $e")
    }

    return patchedLines.joinToString(System.lineSeparator())
        .trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
}

/**
 * Returns the direct child of [ancestor] in this backend's compatibility chain,
 * or this backend itself if it equals [ancestor].
 *
 * This determines the backend name for the patch's `+++ b/` line, ensuring that
 * backends sharing the same patch file (e.g., JKLIB reusing JVM_IR's patch)
 * produce identical patch content.
 */
private fun TargetBackend.directChildOf(ancestor: TargetBackend): TargetBackend {
    var current = this
    while (current != TargetBackend.ANY) {
        if (current.compatibleWith == ancestor) return current
        current = current.compatibleWith
    }
    return this
}

internal fun TestServices.getMatchedBackendFromDirective(directive: ValueDirective<TargetBackend>): TargetBackend? {
    val backendsInDirective = moduleStructure.allDirectives[directive].toSet()
    var current = defaultsProvider.targetBackend ?: return null
    while (current != TargetBackend.ANY) {
        if (current in backendsInDirective) return current
        current = current.compatibleWith
    }
    return null
}

private fun targetSpecificDumpExtension(baseDumpExtension: String, matchedBackend: TargetBackend): String {
    val extensionPrefix = baseDumpExtension.removeSuffix(".txt")
    return "$extensionPrefix.${matchedBackend.name.lowercase()}.patch"
}

internal fun getTargetSpecificDumpExtension(testServices: TestServices, baseDumpExtension: String): String? {
    val matchedBackend = testServices.getMatchedBackendFromDirective(CodegenTestDirectives.DUMP_IR_DIFFERENCE)
        ?: return null
    return targetSpecificDumpExtension(baseDumpExtension, matchedBackend)
}
