/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.getClassifiedDumpFile
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
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
 * @param baseDumpExtension the base dump extension without target override (e.g., "ir.txt" or "kt.txt")
 * @param actualDump the actual dump
 */
internal fun validateTargetSpecificDumpFile(
    testServices: TestServices,
    assertions: Assertions,
    baseDumpExtension: String,
    directiveForIrDifference: ValueDirective<TargetBackend>,
    actualDump: String,
    isKotlinLikeDump: Boolean,
) {
    val moduleStructure = testServices.moduleStructure

    fun assertWithoutPatch() {
        assertions.assertEqualsToDump(moduleStructure, baseDumpExtension, actualDump.ifEmpty { null })
    }

    // Classified patches are not collapsed, so for consistency, they're always against classified dumps.
    val targetBackend = testServices.defaultsProvider.targetBackend ?: return assertWithoutPatch()
    val targetBackendDirectiveName = targetBackend.name
    val dumpDescription = if (isKotlinLikeDump) "Kotlin-like IR dump" else "IR dump"

    val matchedBackend = testServices.getMatchedBackendFromDirective(directiveForIrDifference)
    if (matchedBackend != null) {
        assertions.assertEqualsToDump(
            moduleStructure,
            baseDumpExtension,
            actualDump,
            extraClassifier = matchedBackend.name.lowercase(),
        )
    } else {
        val existingTargetSpecificFile = moduleStructure.findTargetSpecificPatchFile(targetBackend, baseDumpExtension)
        checkTestInfrastructure(existingTargetSpecificFile == null) {
            "Target-specific $dumpDescription file detected but no $directiveForIrDifference directive specified for " +
                    "$targetBackendDirectiveName or its compatible target: $existingTargetSpecificFile"
        }
        if (moduleStructure.allDirectives[directiveForIrDifference].isEmpty()) {
            assertWithoutPatch()
        } else {
            assertions.assertEqualsToDump(moduleStructure, baseDumpExtension, actualDump)
        }
    }
}

private fun TestModuleStructure.findTargetSpecificPatchFile(targetBackend: TargetBackend, baseDumpExtension: String): File? {
    var current = targetBackend
    while (current != TargetBackend.ANY) {
        val ext = targetSpecificDumpExtension(baseDumpExtension, current)
        val file = getClassifiedDumpFile(ext)
        if (file.exists()) {
            return file
        }
        current = current.compatibleWith
    }
    return null
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
