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
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File
import java.util.Locale

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
    val targetBackendName = targetBackend.name.lowercase()
    val targetBackendDirectiveName = targetBackend.name
    val moduleStructure = testServices.moduleStructure
    val dumpDescription = if (isKotlinLikeDump) "Kotlin-like IR dump" else "IR dump"

    val targetSpecificExtension = getTargetSpecificDumpExtension(testServices, baseDumpExtension)
    if (targetSpecificExtension != null) {
        val normalizedActualDump = actualDump.trimEnd()
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

        val mainDump = mainExpectedFile.readText().trimEnd()
        val expectedPatch = buildPatch(
            baseText = mainDump,
            targetText = normalizedActualDump,
            targetBackendName = targetBackendName,
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
        val extensionPrefix = baseDumpExtension.removeSuffix(".txt")
        val baseTestFile = moduleStructure.originalTestDataFiles.first()
        val compatibleBackendNames = targetBackend.compatibleBackendNamesIncludingSelf()
        val possibleTargetFiles = compatibleBackendNames.map { backendName ->
            baseTestFile.withExtension("$extensionPrefix.$backendName.patch")
        }
        val existingTargetSpecificFile = possibleTargetFiles.firstOrNull { it.exists() }

        checkTestInfrastructure (existingTargetSpecificFile == null) {
            "Target-specific $dumpDescription file detected but no DUMP_IR_DIFFERENCE directive specified for $targetBackendDirectiveName: $existingTargetSpecificFile"
        }
        return false
    }
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
        .trimEnd()
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

    return patchedLines.joinToString(System.lineSeparator()).trimEnd()
}

private fun TargetBackend.compatibleBackendNamesIncludingSelf(): List<String> {
    val names = mutableListOf(name.lowercase())
    var current = compatibleWith
    while (current != TargetBackend.ANY) {
        names += current.name.lowercase()
        current = current.compatibleWith
    }
    return names
}

internal fun TestServices.getMatchedBackendFromDirective(directive: ValueDirective<TargetBackend>): TargetBackend? {
    val targetBackend = defaultsProvider.targetBackend ?: return null
    val dumpIrDifferenceBackends = moduleStructure.allDirectives[directive]
    return dumpIrDifferenceBackends
        .filter { targetBackend.isTransitivelyCompatibleWith(it) }
        .minWithOrNull(compareBy<TargetBackend>({ targetBackend.compatibilityDistanceTo(it) }, { it.name.lowercase(Locale.US) }))
}

internal fun getTargetSpecificDumpExtension(testServices: TestServices, baseDumpExtension: String): String? {
    val matchedBackend = testServices.getMatchedBackendFromDirective(CodegenTestDirectives.DUMP_IR_DIFFERENCE)
        ?: return null
    val extensionPrefix = baseDumpExtension.removeSuffix(".txt")
    return "$extensionPrefix.${matchedBackend.name.lowercase()}.patch"
}

private fun TargetBackend.compatibilityDistanceTo(other: TargetBackend): Int {
    var distance = 0
    var current = this
    while (current != TargetBackend.ANY) {
        if (current == other) return distance
        current = current.compatibleWith
        distance++
    }
    return Int.MAX_VALUE
}
