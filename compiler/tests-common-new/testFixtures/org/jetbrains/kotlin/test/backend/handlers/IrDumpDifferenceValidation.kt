/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
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
    mainExpectedFile: File,
    baseDumpExtension: String,
    actualDump: String,
) {
    val targetBackend = testServices.defaultsProvider.targetBackend ?: return
    val targetBackendName = targetBackend.name.lowercase()
    val targetBackendDirectiveName = targetBackend.name
    val moduleStructure = testServices.moduleStructure

    val targetSpecificExtension = getTargetSpecificDumpExtension(testServices, baseDumpExtension)
    if (targetSpecificExtension != null) {
        val targetSpecificFile = moduleStructure.originalTestDataFiles.first()
            .withExtension(targetSpecificExtension)
        val normalizedActualDump = actualDump.trimEnd()
        val mainDump = mainExpectedFile.takeIf { it.exists() }?.readText()?.trimEnd()
        if (targetSpecificFile.exists()) {
            checkTestInfrastructure(normalizedActualDump.isNotEmpty()) {
                "DUMP_IR_DIFFERENCE directive specifies $targetBackendDirectiveName but there is no actual dump"
            }

            val targetSpecificDump = targetSpecificFile.readText().trimEnd()
            if (targetSpecificDump == mainDump) {
                checkTestInfrastructure(targetSpecificFile.delete()) {
                    "Unable to remove redundant target-specific IR dump file: ${targetSpecificFile.absolutePath}"
                }
                testInfraError("There are no IR dump differences. Please remove $targetBackendName from DUMP_IR_DIFFERENCE directive")
            }
        } else {
            if (normalizedActualDump.isNotEmpty()) {
                checkTestInfrastructure (mainExpectedFile.exists()) {
                    "DUMP_IR_DIFFERENCE directive specifies $targetBackendDirectiveName but neither main dump nor target-specific dump exists"
                }
                checkTestInfrastructure(normalizedActualDump != mainDump) {
                    "DUMP_IR_DIFFERENCE directive is specified, but there are no differences between main and target-specific dumps: ${mainExpectedFile.absolutePath}"
                }
            }
        }
    } else {
        val extensionPrefix = baseDumpExtension.removeSuffix(".txt")
        val baseTestFile = moduleStructure.originalTestDataFiles.first()
        val compatibleBackendNames = targetBackend.compatibleBackendNamesIncludingSelf()
        val possibleTargetFiles = compatibleBackendNames.map { backendName ->
            baseTestFile.withExtension("$extensionPrefix.$backendName.txt")
        }
        val existingTargetSpecificFile = possibleTargetFiles.firstOrNull { it.exists() }

        checkTestInfrastructure (existingTargetSpecificFile == null) {
            "Target-specific IR dump file detected but no DUMP_IR_DIFFERENCE directive specified for $targetBackendDirectiveName: $existingTargetSpecificFile"
        }
    }
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

internal fun getTargetSpecificDumpExtension(testServices: TestServices, baseDumpExtension: String): String? {
    val targetBackend = testServices.defaultsProvider.targetBackend ?: return null
    val dumpIrDifferenceBackends = testServices.moduleStructure.allDirectives[CodegenTestDirectives.DUMP_IR_DIFFERENCE]
    val matchedBackend = dumpIrDifferenceBackends
        .filter { targetBackend.isTransitivelyCompatibleWith(it) }
        .minWithOrNull(compareBy<TargetBackend>({ targetBackend.compatibilityDistanceTo(it) }, { it.name.lowercase(Locale.US) }))
        ?: return null
    val extensionPrefix = baseDumpExtension.removeSuffix(".txt")
    return "$extensionPrefix.${matchedBackend.name.lowercase()}.txt"
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
