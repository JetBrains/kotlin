/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel
import java.io.File
import java.util.*
import java.util.regex.Pattern

const val JSPECIFY_NULLNESS_MISMATCH_MARK = "jspecify_nullness_mismatch"
const val JSPECIFY_NULLNESS_NOT_ENOUGH_INFORMATION_MARK = "jspecify_nullness_not_enough_information"
const val JSPECIFY_NULLNESS_INTRINSICALLY_NOT_NULLABLE = "jspecify_nullness_intrinsically_not_nullable"

const val JSPECIFY_NULLABLE_ANNOTATION = "@Nullable"
const val JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION = "@NullnessUnspecified"
const val JSPECIFY_DEFAULT_NOT_NULL_ANNOTATION = "@DefaultNotNull"

private const val JSPECIFY_STATE_SPECIAL_DIRECTIVE = "JSPECIFY_STATE"

abstract class AbstractJspecifyAnnotationsTest : AbstractDiagnosticsTest() {
    open val shouldAutoApplyChanges = System.getProperty("autoApply") == "true" ?: false

    override fun doMultiFileTest(
        wholeFile: File,
        files: List<TestFile>
    ) {
        super.doMultiFileTest(
            wholeFile,
            files,
            MockLibraryUtil.compileJavaFilesLibraryToJar(FOREIGN_ANNOTATIONS_SOURCES_PATH, "foreign-annotations")
        )
    }

    override fun loadLanguageVersionSettings(module: List<TestFile>): LanguageVersionSettings {
        val analysisFlags = mapOf<AnalysisFlag<*>, Any?>(
            JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                ReportLevel.STRICT,
                null,
                mapOf(),
                jspecifyReportLevel = module.getDirectiveValue(JSPECIFY_STATE_SPECIAL_DIRECTIVE) ?: ReportLevel.WARN
            )
        )

        return CompilerTestLanguageVersionSettings(
            DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
            ApiVersion.LATEST_STABLE,
            LanguageVersion.LATEST_STABLE,
            analysisFlags = analysisFlags
        )
    }

    override fun doTest(filePath: String) {
        val ktSourceCode = File(filePath).readText()
        val javaSourcesFilename = javaSourcesPathRegex.matcher(ktSourceCode).also { it.find() }.group(1)
            ?: throw Exception("Java sources' path not found")
        val javaSourcesFile = File("$JSPECIFY_JAVA_SOURCES_PATH/$javaSourcesFilename")
        val mergedSourceCode = buildString {
            appendLine("// ORIGINAL_KT_FILE: $filePath")

            if (javaSourcesFilename.endsWith(".java")) {
                appendLine(makeJavaClassesPublicAndSeparatedByFiles(javaSourcesFile.readText()))
            } else {
                assert(javaSourcesFile.isDirectory) { "Specified Java sources should be a file with `java` extension or directory" }

                for (javaFile in javaSourcesFile.walkTopDown().filter { it.isFile && it.extension == "java" }) {
                    appendLine("// FILE: ${javaFile.name}")
                    appendLine(makeJavaClassesPublicAndSeparatedByFiles(javaFile.readText()))
                }
            }
            appendLine("// FILE: main.kt\n$ktSourceCode")
        }

        super.doTest(createTempFile().apply { writeText(mergedSourceCode) }.path)
    }

    private fun makeJavaClassesPublicAndSeparatedByFiles(javaCode: String): String {
        val importSectionMatch = importSectionRegex.find(javaCode)
        val importSection = if (importSectionMatch != null) "\n${importSectionMatch.groups[1]!!.value}\n" else ""

        return javaCode.replace(importSectionRegex, "")
            .replace(publicClassOrInterfaceRegex, "$1")
            .replace(classOrInterfaceRegex, "public $1")
            .replace(classShapeRegex, "\n// FILE: $3.java$importSection$1\npublic $2 $3$4")
    }

    private fun getJspecifyMarkRegex(jspecifyMark: String) = Regex("""^\s*// $jspecifyMark$""")

    private fun checkIfAllJspecifyMarksByDiagnosticsArePresent(
        diagnosedRanges: List<DiagnosedRange>,
        lineIndexesByRanges: TreeMap<Int, Int>,
        textLines: List<String>,
        compilerDiagnosticsToJspecifyMarksMap: Map<String, String>
    ) {
        for (diagnosticRange in diagnosedRanges) {
            val lineIndex = lineIndexesByRanges.floorEntry(diagnosticRange.start).value

            for (diagnostic in diagnosticRange.getDiagnostics()) {
                val requiredJspecifyMark = compilerDiagnosticsToJspecifyMarksMap[diagnostic.name] ?: continue

                fun getErrorMessage(lineIndex: Int) =
                    "Jspecify mark '$requiredJspecifyMark' not found for diagnostic '${diagnostic}' at ${lineIndex + 1} line.\n" +
                            "It should be located at the previous line as a comment."

                assert(lineIndex != 0) { getErrorMessage(0) }

                val previousLine = textLines[lineIndex - 1]

                assert(getJspecifyMarkRegex(requiredJspecifyMark).matches(previousLine)) { getErrorMessage(lineIndex) }
            }
        }
    }

    private fun checkIfAllDiagnosticsByJspecifyMarksArePresent(
        diagnosedRanges: List<DiagnosedRange>,
        lineIndexesByRanges: TreeMap<Int, Int>,
        textLines: List<String>,
        compilerDiagnosticsToJspecifyMarksMap: Map<String, List<String>>
    ) {
        for ((jspecifyMark, possibleDiagnostics) in compilerDiagnosticsToJspecifyMarksMap) {
            val diagnosticRanges = diagnosedRanges.mapNotNull {
                val relevantDiagnostics = it.getDiagnostics().filter { it.name in possibleDiagnostics }
                if (relevantDiagnostics.isEmpty()) return@mapNotNull null
                it.start
            }

            val lineIndexesWithJspecifyMarks =
                textLines.mapIndexedNotNull { index, it -> getJspecifyMarkRegex(jspecifyMark).find(it)?.let { index } }

            if (diagnosticRanges.isEmpty()) {
                if (lineIndexesWithJspecifyMarks.isEmpty()) {
                    continue
                } else {
                    fail(
                        "None of \"${possibleDiagnostics.joinToString(", ")}\" diagnostics not found " +
                                "for jspecify mark '$jspecifyMark' at lines: ${lineIndexesWithJspecifyMarks.map { it + 1 }.joinToString()}"
                    )
                }
            }

            for (lineIndex in lineIndexesWithJspecifyMarks) {
                val lineStartPosition = lineIndexesByRanges.entries.find { (_, index) -> index == lineIndex + 1 }?.key
                val errorMessage = "None of \"${possibleDiagnostics.joinToString()}\" diagnostics not found " +
                        "for jspecify mark '$jspecifyMark' at ${lineIndex + 1} line"

                assertNotNull(errorMessage, lineStartPosition)

                val lineEndPosition = lineStartPosition!! + textLines[lineIndex + 1].length
                val isCorrespondingDiagnosticPresent = diagnosticRanges.any { it in lineStartPosition..lineEndPosition }

                assertTrue(errorMessage, isCorrespondingDiagnosticPresent)
            }
        }
    }

    override fun checkDiagnostics(actualText: String, testDataFile: File, testFiles: List<BaseDiagnosticsTest.TestFile>) {
        val mergedTestFilePath = originalKtFileRegex.matcher(actualText).also { it.find() }.group(1)
            ?: throw Exception("Path for original kt file in the merged file not found")

        val textWithDiagnostics = actualText.substringAfter(MAIN_KT_FILE_DIRECTIVE).removeSuffix("\n")
        val diagnosedRanges = mutableListOf<DiagnosedRange>()
        val textWithoutDiagnostics = CheckerTestUtil.parseDiagnosedRanges(textWithDiagnostics, diagnosedRanges)

        val textLines = textWithoutDiagnostics.lines()
        val lineIndexesByRanges = TreeMap<Int, Int>().apply {
            textLines.scanIndexed(0) { index, position, line ->
                put(position, index)
                position + line.length + 1 // + new line symbol
            }
        }

        val jspecifyMode = testFiles.getDirectiveValue(JSPECIFY_STATE_SPECIAL_DIRECTIVE)
            ?: JavaTypeEnhancementState.DEFAULT_REPORT_LEVEL_FOR_JSPECIFY
        val compilerDiagnosticsToJspecifyMarksMap = when (jspecifyMode) {
            ReportLevel.STRICT -> diagnosticsToJspecifyMarksMapForStrictMode
            ReportLevel.WARN -> diagnosticsToJspecifyMarksMapForWarnMode
            ReportLevel.IGNORE -> mapOf()
        }
        val jspecifyMarksToCompilerDiagnosticsMap = when (jspecifyMode) {
            ReportLevel.STRICT -> jspecifyMarksToPossibleDiagnosticsForStrictMode
            ReportLevel.WARN -> jspecifyMarksToPossibleDiagnosticsForWarnMode
            ReportLevel.IGNORE -> mapOf()
        }

        if (shouldAutoApplyChanges) {
            File(mergedTestFilePath).writeText(textWithDiagnostics)
        }

        super.checkDiagnostics(textWithDiagnostics, File(mergedTestFilePath), testFiles)

        checkIfAllJspecifyMarksByDiagnosticsArePresent(
            diagnosedRanges,
            lineIndexesByRanges,
            textLines,
            compilerDiagnosticsToJspecifyMarksMap
        )
        checkIfAllDiagnosticsByJspecifyMarksArePresent(
            diagnosedRanges,
            lineIndexesByRanges,
            textLines,
            jspecifyMarksToCompilerDiagnosticsMap
        )
    }

    override fun getExpectedDescriptorFile(testDataFile: File, files: List<TestFile>): File {
        val originalKtFilePath = originalKtFileRegex.matcher(testDataFile.readText()).also { it.find() }.group(1)
            ?: throw Exception("Path for original kt file in the merged file isn't found")

        return File(FileUtil.getNameWithoutExtension(originalKtFilePath) + ".txt")
    }

    companion object {
        const val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/jdk8-annotations"
        const val JSPECIFY_JAVA_SOURCES_PATH = "compiler/testData/foreignAnnotationsJava8/tests/jspecify/java"
        const val MAIN_KT_FILE_DIRECTIVE = "// FILE: main.kt\n"

        private val originalKtFileRegex = Pattern.compile("""// ORIGINAL_KT_FILE: (.*?\.kts?)\n""")
        private val javaSourcesPathRegex = Pattern.compile("""// JAVA_SOURCES: (.*?(?:\.java)?)\n""")

        val diagnosticsToJspecifyMarksMapForWarnMode = mapOf(
            NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.name to "jspecify_nullness_mismatch",
        )

        val jspecifyMarksToPossibleDiagnosticsForWarnMode =
            diagnosticsToJspecifyMarksMapForWarnMode.entries.groupBy({ it.value }, { it.key })

        val diagnosticsToJspecifyMarksMapForStrictMode = mapOf(
            TYPE_MISMATCH.name to "jspecify_nullness_mismatch",
            NULL_FOR_NONNULL_TYPE.name to "jspecify_nullness_mismatch",
            NOTHING_TO_OVERRIDE.name to "jspecify_nullness_mismatch",
            RETURN_TYPE_MISMATCH_ON_OVERRIDE.name to "jspecify_nullness_mismatch",
            UPPER_BOUND_VIOLATED.name to "jspecify_nullness_mismatch",
        )

        val jspecifyMarksToPossibleDiagnosticsForStrictMode =
            diagnosticsToJspecifyMarksMapForStrictMode.entries.groupBy({ it.value }, { it.key })

        private val importSectionRegex = Regex("""((?:import .*?;\n)+)""")
        private val classOrInterfaceRegex = Regex("""(class|interface)""")
        private val publicClassOrInterfaceRegex = Regex("""public (class|interface)""")
        private val classShapeRegex = Regex("""(\n@DefaultNonNull)?\npublic (class|interface) (\w+)(<[^>]+>)?""")
    }
}
