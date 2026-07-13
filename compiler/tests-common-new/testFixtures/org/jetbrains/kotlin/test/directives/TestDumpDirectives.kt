/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AnalysisHandlerBase
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File

object TestDumpDirectives : SimpleDirectivesContainer() {
    /**
     * Sets a "classifier" to be used when calculating the path of dump files.
     * This classifier should be placed before the dump extension, allowing variants of the same dump file to be generated.
     * This allows multiple runners to dump the same file but with variations depending on the runner configuration.
     */
    val DUMP_CLASSIFIER by stringDirective(
        description = "The test runner classifier for dump files."
    )
}

/**
 * Converts the received [File] to use the specified extension according to dump file conventions.
 * Conversion is performed ***without*** the use of a [classifier][TestDumpDirectives.DUMP_CLASSIFIER].
 *
 * This is an infrastructure internal function and should be handled with care.
 *
 * @see toClassifiedDumpFile
 */
@TestInfrastructureInternals
fun File.toDefaultDumpFile(extension: String): File {
    return withExtension(extension)
}

/**
 * Gets the dump [File] ***without*** a [classifier][TestDumpDirectives.DUMP_CLASSIFIER] from the [TestModuleStructure].
 *
 * @see getClassifiedDumpFile
 */
@OptIn(TestInfrastructureInternals::class)
fun TestModuleStructure.getDefaultDumpFile(extension: String): File {
    return originalTestDataFiles.first().toDefaultDumpFile(extension)
}

/**
 * Converts the received [File] to use the specified extension according to dump file conventions.
 * Conversion is performed ***with*** the use of a [classifier][TestDumpDirectives.DUMP_CLASSIFIER].
 *
 * This is an infrastructure internal function and should be handled with care.
 */
@TestInfrastructureInternals
fun File.toClassifiedDumpFile(extension: String, directives: RegisteredDirectives): File {
    val classifier = directives[TestDumpDirectives.DUMP_CLASSIFIER].lastOrNull() ?: ""
    return withExtension("${classifier.removePrefix(".")}.${extension.removePrefix(".")}")
}

/**
 * Gets the dump [File] ***with*** a [classifier][TestDumpDirectives.DUMP_CLASSIFIER] from the [TestModuleStructure].
 */
@OptIn(TestInfrastructureInternals::class)
fun TestModuleStructure.getClassifiedDumpFile(extension: String): File {
    return originalTestDataFiles.first().toClassifiedDumpFile(extension, allDirectives)
}

/**
 * Asserts that [actualDump] matches the [default][getDefaultDumpFile] or [classified][getClassifiedDumpFile] dump file in the module structure.
 * If the default dump file exists and matches [actualDump], this function ensures the classified dump file does not exist.
 * When [actualDump] does not match the default dump file, it is compared against the classified dump file.
 * If [actualDump] is `null`, this function ensures that the classified dump file does not exist.
 */
fun Assertions.assertEqualsToDump(
    moduleStructure: TestModuleStructure,
    extension: String,
    actualDump: String?,
    sanitizer: (String) -> String = { it },
) {
    val classifiedDumpFile = moduleStructure.getClassifiedDumpFile(extension)
    if (actualDump == null) {
        assertFileDoesntExist(classifiedDumpFile) { "Dump file detected but nothing to dump: ${classifiedDumpFile.name}" }
        return
    }

    fun String.sanitize() = sanitizer.invoke(trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF())

    val defaultDumpFile = moduleStructure.getDefaultDumpFile(extension)
    if (defaultDumpFile.path != classifiedDumpFile.path &&
        defaultDumpFile.exists() &&
        defaultDumpFile.readText().sanitize() == actualDump.sanitize()
    ) {
        assertFileDoesntExist(classifiedDumpFile) { "No need for a separate dump file: ${classifiedDumpFile.name}" }
    } else {
        assertEqualsToFile(classifiedDumpFile, actualDump, sanitizer)
    }
}

/**
 * Asserts that [actualDump] matches the [default][getDefaultDumpFile] or [classified][getClassifiedDumpFile] dump file in the module structure.
 * If the default dump file exists and matches [actualDump], this function ensures the classified dump file does not exist.
 * When [actualDump] does not match the default dump file, it is compared against the classified dump file.
 * If [actualDump] is `null`, this function ensures that the classified dump file does not exist.
 */
context(base: AnalysisHandlerBase<*>)
fun assertEqualsToDump(
    extension: String,
    actualDump: String?,
    sanitizer: (String) -> String = { it },
) {
    base.testServices.assertions.assertEqualsToDump(base.testServices.moduleStructure, extension, actualDump, sanitizer)
}
