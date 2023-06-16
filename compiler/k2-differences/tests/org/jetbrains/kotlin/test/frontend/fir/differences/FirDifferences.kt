/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.test.util.LANGUAGE_FEATURE_PATTERN
import java.io.File
import java.io.Writer

val equivalentDiagnostics = listOf(
    listOf("PARCELABLE_CANT_BE_LOCAL_CLASS", "PARCELABLE_SHOULD_BE_CLASS"),
    listOf(
        "TYPE_MISMATCH",
        "ARGUMENT_TYPE_MISMATCH",
        "RETURN_TYPE_MISMATCH",
        "ASSIGNMENT_TYPE_MISMATCH",
        "INITIALIZER_TYPE_MISMATCH",
        "NULL_FOR_NONNULL_TYPE",
        "TYPE_MISMATCH_IN_FOR_LOOP",
        "TYPE_MISMATCH_IN_RANGE",
        "CONSTANT_EXPECTED_TYPE_MISMATCH",
        "HAS_NEXT_FUNCTION_TYPE_MISMATCH",
        "CONDITION_TYPE_MISMATCH",
        "EXPECTED_TYPE_MISMATCH",
        "EXPECTED_PARAMETERS_NUMBER_MISMATCH",
        "TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN",
        "SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED",
        "UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION",
        "UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS",
        "UPPER_BOUND_VIOLATED",
        "NEW_INFERENCE_ERROR",
        "NO_VALUE_FOR_PARAMETER",
        "NO_SET_METHOD",
        "SETTER_PROJECTED_OUT",
//    ),
//    listOf(
        "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
        "UNRESOLVED_REFERENCE",
        "UNRESOLVED_MEMBER",
        "UNRESOLVED_IMPORT",
        "UNRESOLVED_LABEL",
        "NO_THIS",
        "TOO_MANY_ARGUMENTS",
        "UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE",
        "VARARG_OUTSIDE_PARENTHESES",
        "CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY",
        "DEPRECATION_ERROR",
        "OVERLOAD_RESOLUTION_AMBIGUITY",
        "NO_COMPANION_OBJECT",
        "NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE",
        "RESOLUTION_TO_CLASSIFIER",
        "FUNCTION_EXPECTED",
        "ENUM_ENTRY_AS_TYPE",
        "RECURSIVE_TYPEALIAS_EXPANSION",
        "MISSING_DEPENDENCY_CLASS",
        "CANNOT_INFER_PARAMETER_TYPE",
        "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER",
//    ),
//    listOf(
        "INVISIBLE_MEMBER",
        "INVISIBLE_REFERENCE",
//    ),
//    listOf(
        "WRONG_NUMBER_OF_TYPE_ARGUMENTS",
        "NO_TYPE_ARGUMENTS_ON_RHS",
        "CANNOT_CHECK_FOR_ERASED",
        "TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED",
        "INAPPLICABLE_CANDIDATE",
//    ),
//    listOf(
        "VAL_REASSIGNMENT",
        "NONE_APPLICABLE",
        "CAPTURED_VAL_INITIALIZATION",
    ),
    listOf("DELEGATE_SPECIAL_FUNCTION_MISSING", "DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE"),
    listOf(
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR",
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM",
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR",
    ),
    listOf(
        "ASSIGNMENT_IN_EXPRESSION_CONTEXT",
        "EXPRESSION_EXPECTED",
        "EXPRESSION_REQUIRED",
    ),
    listOf(
        "SMARTCAST_IMPOSSIBLE",
        "UNSAFE_CALL",
    ),
    listOf(
        "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
        "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    ),
    listOf(
        "RETURN_NOT_ALLOWED",
        "NOT_A_FUNCTION_LABEL",
    ),
    listOf(
        "NO_ACTUAL_FOR_EXPECT",
        "INCOMPATIBLE_MATCHING",
    ),
    listOf(
        "UNINITIALIZED_VARIABLE",
        "UNINITIALIZED_ENUM_COMPANION",
    ),
)

val k2SpecificLanguageFeatures = listOf(
    LanguageFeature.ReportErrorsForComparisonOperators,
)

val equivalentDiagnosticsLookup = buildMap {
    for (klass in equivalentDiagnostics) {
        for (diagnostic in klass) {
            if (diagnostic in this) {
                error("$diagnostic is present both in ${this[diagnostic]} and in $klass")
            }

            put(diagnostic, klass)
        }
    }
}

val ParsedCodeMetaInfo.equivalenceClass: Any get() = equivalentDiagnosticsLookup[tag] ?: tag

fun ParsedCodeMetaInfo.isOnSamePositionAs(another: ParsedCodeMetaInfo) =
    start == another.start && end == another.end

fun ParsedCodeMetaInfo.isEquivalentTo(another: ParsedCodeMetaInfo): Boolean {
    return when (another) {
        this -> true
        else -> equivalenceClass == another.equivalenceClass && isOnSamePositionAs(another)
    }
}

fun extractCommonMetaInfosSlow(
    allK1MetaInfos: Collection<ParsedCodeMetaInfo>,
    allK2MetaInfos: Collection<ParsedCodeMetaInfo>,
): Triple<Collection<ParsedCodeMetaInfo>, Collection<ParsedCodeMetaInfo>, Collection<ParsedCodeMetaInfo>> {
    val (commonMetaInfos, k1MetaInfos) = allK1MetaInfos.partition { tagInK1 -> allK2MetaInfos.any { it.isEquivalentTo(tagInK1) } }
    val k2MetaInfos = allK2MetaInfos.filter { tagInK2 -> commonMetaInfos.none { it.isEquivalentTo(tagInK2) } }
    return Triple(k1MetaInfos, k2MetaInfos, commonMetaInfos)
}

fun extractSignificantMetaInfosSlow(
    erroneousK1MetaInfos: Collection<ParsedCodeMetaInfo>,
    erroneousK2MetaInfos: Collection<ParsedCodeMetaInfo>,
): Pair<Collection<ParsedCodeMetaInfo>, Collection<ParsedCodeMetaInfo>> {
    val (k1MetaInfos, k2MetaInfos, commonMetaInfos) = extractCommonMetaInfosSlow(erroneousK1MetaInfos, erroneousK2MetaInfos)

    val significantK1MetaInfo = k1MetaInfos.filter { tagInK1 ->
        commonMetaInfos.none { it.isOnSamePositionAs(tagInK1) }
    }
    val significantK2MetaInfo = k2MetaInfos.filter { tagInK2 ->
        commonMetaInfos.none { it.isOnSamePositionAs(tagInK2) }
    }

    return significantK1MetaInfo to significantK2MetaInfo
}

fun extractCommonMetaInfos(
    allK1MetaInfos: Collection<ParsedCodeMetaInfo>,
    allK2MetaInfos: Collection<ParsedCodeMetaInfo>,
): Triple<MetaInfoTreeSet, MetaInfoTreeSet, MetaInfoTreeSet> {
    val k1MetaInfos = MetaInfoTreeSet().also {
        it.addAll(allK1MetaInfos)
    }

    val commonMetaInfos = MetaInfoTreeSet()
    val k2MetaInfos = MetaInfoTreeSet()

    for (metaInfo in allK2MetaInfos) {
        if (metaInfo in k1MetaInfos) {
            commonMetaInfos.add(metaInfo)
            k1MetaInfos.remove(metaInfo)
        } else {
            k2MetaInfos.add(metaInfo)
        }
    }

    return Triple(k1MetaInfos, k2MetaInfos, commonMetaInfos)
}

fun extractSignificantMetaInfos(
    erroneousK1MetaInfos: Collection<ParsedCodeMetaInfo>,
    erroneousK2MetaInfos: Collection<ParsedCodeMetaInfo>,
): Pair<Collection<ParsedCodeMetaInfo>, Collection<ParsedCodeMetaInfo>> {
    val (k1MetaInfos, k2MetaInfos, commonMetaInfos) = extractCommonMetaInfos(erroneousK1MetaInfos, erroneousK2MetaInfos)

    val significantK1MetaInfo = k1MetaInfos.filterNot { tagInK1 ->
        commonMetaInfos.hasDiagnosticsAt(tagInK1.start, tagInK1.end)
    }
    val significantK2MetaInfo = k2MetaInfos.filterNot { tagInK2 ->
        commonMetaInfos.hasDiagnosticsAt(tagInK2.start, tagInK2.end)
    }

    return significantK1MetaInfo to significantK2MetaInfo
}

val MAGIC_DIAGNOSTICS_THRESHOLD = 80

val k1DefinitelyNonErrors = collectAllK1NonErrors()
val k2DefinitelyNonErrors = collectAllK2NonErrors()

val k1JvmDefinitelyNonErrors = collectFromFieldsOf(ErrorsJvm::class, instance = null, collector = ::getErrorFromFieldValue)

val k2KnownErrors = collectAllK2Errors()

val ParsedCodeMetaInfo.isProbablyK1Error get() = !tag.startsWith("DEBUG_INFO_") && tag !in k1DefinitelyNonErrors
val ParsedCodeMetaInfo.isProbablyK2Error get() = !tag.startsWith("DEBUG_INFO_") && tag !in k2DefinitelyNonErrors

fun IntArray.getLineNumberForOffset(offset: Int): Int {
    return indexOfLast { it <= offset } + 1
}

fun IntArray.getLineNumberForOffsetBinary(offset: Int): Int {
    val index = binarySearch(offset)
    return when (val isInsertionPoint = index < 0) {
        isInsertionPoint -> -index - 1
        else -> index + 1
    }
}

class ModifiedParsedCodeMetaInfo(
    start: Int,
    end: Int,
    attributes: MutableList<String>,
    tag: String,
    description: String?,
    val original: ParsedCodeMetaInfo,
) : ParsedCodeMetaInfo(start, end, attributes, tag, description)

fun ParsedCodeMetaInfo.replaceOffsetsWithLineNumbersWithin(lineStartOffsets: IntArray): ParsedCodeMetaInfo {
    val lineIndex = lineStartOffsets.getLineNumberForOffset(start)
    return ModifiedParsedCodeMetaInfo(lineIndex + 1, lineIndex + 1, attributes, tag, description, this)
}

val ParsedCodeMetaInfo.originalOrSelf
    get() = when (this) {
        is ModifiedParsedCodeMetaInfo -> original
        else -> this
    }

val String.lineStartOffsets: IntArray
    get() {
        val buffer = mutableListOf<Int>()
        var currentOffset = 0

        split("\n").forEach { line ->
            buffer.add(currentOffset)
            currentOffset += line.length + 1
        }

        buffer.add(currentOffset)
        return buffer.toIntArray()
    }

class EquivalenceTestResult(
    val significantK1MetaInfo: Collection<ParsedCodeMetaInfo>,
    val significantK2MetaInfo: Collection<ParsedCodeMetaInfo>,
)

fun reportEquivalenceDifference(
    writer: Writer,
    result: EquivalenceTestResult,
    relativeTestPath: String,
) {
    writer.write("The `${relativeTestPath}` test:\n\n")

    for (it in result.significantK1MetaInfo) {
        writer.write("- `#potential-feature`: `${it.tag}` was in K1 at `(${it.start}..${it.end})`, but disappeared\n")
    }

    for (it in result.significantK2MetaInfo) {
        writer.write("- `#potential-breaking-change`: `${it.tag}` was introduced in K2 at `(${it.start}..${it.end})`\n")
    }

    writer.write("\n")
}

inline fun EquivalenceTestResult.ifTests(
    testFile: File,
    writer: Writer,
    relativeK1TestPath: String,
    relativeK2TestPath: String,
    areNonEquivalent: (EquivalenceTestResult) -> Unit,
    collectObsoleteFeatures: () -> List<String>,
    collectBrandNewFeatures: () -> List<String>,
) {
    val areEquivalent = significantK1MetaInfo.isEmpty() && significantK2MetaInfo.isEmpty()

    if (!areEquivalent) {
        val obsoleteFeatures = collectObsoleteFeatures()

        if (obsoleteFeatures.isNotEmpty()) {
            writer.write("The `${relativeK1TestPath}` tests are not really equivalent, but the K1 file disables the `$obsoleteFeatures` features which K2 is not expected to support anyway, because they become stable before 2.0.\n\n")
            return
        }

        if (
            !"testsWithJvmBackend".substring.matches(testFile.path) &&
            significantK1MetaInfo.isNotEmpty() &&
            significantK1MetaInfo.all { it.tag in k1JvmDefinitelyNonErrors }
        ) {
            val jvmErrors = significantK1MetaInfo.map { it.tag }
            writer.write("The `${relativeK1TestPath}` tests are not really equivalent, but the K1 file only contains JVM-specific errors (`${jvmErrors}`), and this test is not placed in the correct folder for jvm-specific tests\n\n")
            return
        }

        val brandNewFeatures = collectBrandNewFeatures()

        if (brandNewFeatures.isNotEmpty()) {
            writer.write("The `${relativeK2TestPath}` tests are not really equivalent, but the K2 file enables the `$brandNewFeatures` features which K1 is not expected to support.\n\n")
            return
        }

        reportEquivalenceDifference(writer, this, relativeK1TestPath)
        areNonEquivalent(this)
    }
}

fun analyseEquivalencesAmong(
    allK1MetaInfos: List<ParsedCodeMetaInfo>,
    allK2MetaInfos: List<ParsedCodeMetaInfo>,
): EquivalenceTestResult {
    val (significantK1MetaInfo, significantK2MetaInfo) = when {
        allK1MetaInfos.size + allK2MetaInfos.size <= MAGIC_DIAGNOSTICS_THRESHOLD -> {
            extractSignificantMetaInfosSlow(allK1MetaInfos, allK2MetaInfos)
        }
        else -> {
            extractSignificantMetaInfos(allK1MetaInfos, allK2MetaInfos)
        }
    }

    return EquivalenceTestResult(significantK1MetaInfo, significantK2MetaInfo)
}

fun analyseEquivalencesAsHierarchyAmong(
    erroneousK1MetaInfos: List<ParsedCodeMetaInfo>,
    erroneousK2MetaInfos: List<ParsedCodeMetaInfo>,
): EquivalenceTestResult {
    val allK1MetaInfos = MetaInfoHierarchySet().also {
        it.addAll(erroneousK1MetaInfos)
    }
    val allK2MetaInfos = MetaInfoHierarchySet().also {
        it.addAll(erroneousK2MetaInfos)
    }

    val significantK1MetaInfo = erroneousK1MetaInfos.filterNot {
        allK2MetaInfos.hasOverlappingEquivalentOf(it)
    }
    val significantK2MetaInfo = erroneousK2MetaInfos.filterNot {
        allK1MetaInfos.hasOverlappingEquivalentOf(it)
    }

    return EquivalenceTestResult(significantK1MetaInfo, significantK2MetaInfo)
}

fun collectLanguageFeatures(
    text: String,
    filePath: String,
    requiredMatcher: String,
    condition: (LanguageFeature) -> Boolean,
): List<String> {
    val languageString = """// ?!?LANGUAGE:\s*(.*)""".toRegex().find(text)?.groupValues
        ?: return emptyList()

    return languageString[1].split("""\s+""".toRegex()).filter { featureString ->
        val matcher = LANGUAGE_FEATURE_PATTERN.matcher(featureString)
        if (!matcher.find()) {
            error("Invalid language feature pattern: $featureString (of ${languageString.first()} in $filePath)")
        }
        if (matcher.group(1) != requiredMatcher) {
            return@filter false
        }
        val name = matcher.group(2)
        val feature = LanguageFeature.fromString(name) ?: error("No such language feature found: $name")
        condition(feature)
    }
}

fun collectObsoleteDisabledLanguageFeatures(
    text: String,
    filePath: String,
) = collectLanguageFeatures(text, filePath, "-") { feature ->
    feature.sinceVersion?.let { it <= LanguageVersion.KOTLIN_2_0 } == true
}

fun collectBrandNewEnabledLanguageFeatures(
    text: String,
    filePath: String,
) = collectLanguageFeatures(text, filePath, "+") { feature ->
    feature in k2SpecificLanguageFeatures
}

val specTestCommentPattern = """/\*\n \* .*SPEC TEST""".toRegex()
val testCaseCommentPattern = """TESTCASE NUMBER""".toRegex()

fun fixMissingTestSpecComments(
    alongsideNonIdenticalTests: List<String>,
) {
    val status = StatusPrinter()

    val missingCommentsCount = alongsideNonIdenticalTests.count {
        status.loading("Checking SPEC TEST comments in $it", probability = 0.001)
        val k1Text = File(it).readText()
        val indexOfSpecTestInK1 = specTestCommentPattern.find(k1Text)?.range?.first ?: return@count false

        val k2File = File(it).analogousK2File
        val k2Text = k2File.readText()

        if ("SPEC TEST" in k2Text) {
            return@count false
        }

        status.loading("Writing SPEC TEST comments in $it")

        val indexOfTestCaseInK1 = testCaseCommentPattern.find(k1Text)?.range?.first
            ?: error("Couldn't find the TESTCASE comment in $it")
        val indexOfTestCaseInK2 = testCaseCommentPattern.find(k2Text)?.range?.first
            ?: error("Couldn't find the TESTCASE comment in ${k2File.path}")

        k2File.writeText(
            k2Text.substring(0, indexOfSpecTestInK1) +
                    k1Text.subSequence(indexOfSpecTestInK1, indexOfTestCaseInK1) +
                    k2Text.substring(indexOfTestCaseInK2)
        )

        true
    }

    status.done("$missingCommentsCount fir files had missing TEST SPEC comments")
}

class DiagnosticsStatistics(
    val disappearedDiagnosticToFilesCount: MutableMap<String, Int> = mutableMapOf(),
    val introducedDiagnosticToFilesCount: MutableMap<String, Int> = mutableMapOf(),
)

fun DiagnosticsStatistics.recordDiagnosticsStatistics(result: EquivalenceTestResult) {
    for (it in result.significantK1MetaInfo) {
        val disappearedCount = disappearedDiagnosticToFilesCount.getOrDefault(it.tag, 0)
        disappearedDiagnosticToFilesCount[it.tag] = disappearedCount + 1
    }

    for (it in result.significantK2MetaInfo) {
        val introducedCount = introducedDiagnosticToFilesCount.getOrDefault(it.tag, 0)
        introducedDiagnosticToFilesCount[it.tag] = introducedCount + 1
    }
}

fun printDiagnosticsStatistics(title: String? = null, diagnostics: Map<String, Int>, writer: Writer) {
    if (title != null) {
        writer.write("$title\n\n")
    }

    val sorted = diagnostics.entries.sortedByDescending { it.value }

    for (it in sorted) {
        writer.write("- `${it.key}`: ${it.value} files")

        val knownIssue = knownMissingDiagnostics[it.key]

        if (knownIssue != null) {
            writer.write(". The corresponding issue is KT-${knownIssue.numberInProject}\n")
        } else {
            writer.write("\n")
        }
    }
}

fun File.renderDiagnosticsStatistics(diagnosticsStatistics: DiagnosticsStatistics) {
    bufferedWriter().use { writer ->
        printDiagnosticsStatistics(
            "Most common reasons of potential features (by the number of files) include:",
            diagnosticsStatistics.disappearedDiagnosticToFilesCount,
            writer,
        )
        writer.write("\n")
        printDiagnosticsStatistics(
            "Most common reasons of breaking changes (by the number of files) include:",
            diagnosticsStatistics.introducedDiagnosticToFilesCount,
            writer,
        )
    }
}

val KT_ISSUE_PATTERN = """KT-?\d+""".toRegex(RegexOption.IGNORE_CASE)

fun main() {
    val projectDirectory = File(System.getProperty("user.dir"))
    val build = projectDirectory.child("compiler").child("k2-differences").child("build")

    val tests = deserializeOrGenerate(build.child("testsStats.json")) {
        collectTestsStats(projectDirectory)
    }

    fixMissingTestSpecComments(tests.alongsideNonIdenticalTests)
    val status = StatusPrinter()

    fun Int.outOfAllAlongsideTests(): Int = this * 100 / (tests.alongsideNonIdenticalTests.size + tests.alongsideIdenticalTests.size)

    status.done("${tests.alongsideNonIdenticalTests.size} tests are non-identical among the total of ${tests.alongsideNonIdenticalTests.size + tests.alongsideIdenticalTests.size} alongside tests (~${tests.alongsideNonIdenticalTests.size.outOfAllAlongsideTests()}%)")

    val alongsideNonEquivalentTests = mutableListOf<File>()
    val alongsideNonSimilarTests = mutableListOf<File>()
    val alongsideNonContainedTests = mutableListOf<File>()
    val alongsideNonContainedTestsWithIssues = mutableMapOf<File, String>()

    val similarityStatistics = DiagnosticsStatistics()
    val containmentStatistics = DiagnosticsStatistics()

    fun checkTest(
        testPath: String,
        equivalence: Writer,
        similarity: Writer,
        containment: Writer,
    ) {
        status.loading("Checking $testPath", probability = 0.01)
        val test = File(testPath)
        val relativeK1TestPath = testPath.removePrefix(projectDirectory.path)
        val relativeK2TestPath = test.analogousK2File.path.removePrefix(projectDirectory.path)

        val k1Text = test.readText()
        val k2Text = test.analogousK2File.readText()

        val obsoleteFeatures by lazy {
            collectObsoleteDisabledLanguageFeatures(k1Text, relativeK1TestPath)
        }

        val brandNewFeatures by lazy {
            collectBrandNewEnabledLanguageFeatures(k2Text, relativeK2TestPath)
        }

        val allK1MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(k1Text).filter { it.isProbablyK1Error }
        val allK2MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(k2Text).filter { it.isProbablyK2Error }

        analyseEquivalencesAmong(allK1MetaInfos, allK2MetaInfos).ifTests(
            test, equivalence, relativeK1TestPath, relativeK2TestPath,
            areNonEquivalent = { _ ->
                alongsideNonEquivalentTests.add(test)
            },
            collectObsoleteFeatures = { obsoleteFeatures },
            collectBrandNewFeatures = { brandNewFeatures },
        )

        val k1LineStartOffsets = clearTextFromDiagnosticMarkup(k1Text).lineStartOffsets
        val k2LineStartOffsets = clearTextFromDiagnosticMarkup(k2Text).lineStartOffsets

        val allK1LineMetaInfos = allK1MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k1LineStartOffsets) }
        val allK2LineMetaInfos = allK2MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k2LineStartOffsets) }

        val similarityAnalysisResults = analyseEquivalencesAmong(allK1LineMetaInfos, allK2LineMetaInfos)

        similarityAnalysisResults.ifTests(
            test, similarity, relativeK1TestPath, relativeK2TestPath,
            areNonEquivalent = { result ->
                alongsideNonSimilarTests.add(test)
                similarityStatistics.recordDiagnosticsStatistics(result)
            },
            collectObsoleteFeatures = { obsoleteFeatures },
            collectBrandNewFeatures = { brandNewFeatures },
        )

        val nonSimilarK1MetaInfos = similarityAnalysisResults.significantK1MetaInfo.map { it.originalOrSelf }
        val nonSimilarK2MetaInfos = similarityAnalysisResults.significantK2MetaInfo.map { it.originalOrSelf }

        analyseEquivalencesAsHierarchyAmong(nonSimilarK1MetaInfos, nonSimilarK2MetaInfos).ifTests(
            test, containment, relativeK1TestPath, relativeK2TestPath,
            areNonEquivalent = { result ->
                alongsideNonContainedTests.add(test)
                containmentStatistics.recordDiagnosticsStatistics(result)

                KT_ISSUE_PATTERN.find(k2Text)?.groupValues?.first()?.let {
                    alongsideNonContainedTestsWithIssues[test] = it
                }
            },
            collectObsoleteFeatures = { obsoleteFeatures },
            collectBrandNewFeatures = { brandNewFeatures },
        )
    }

    build.child("similarity-report.md").bufferedWriter().use { similarity ->
        build.child("equivalence-report.md").bufferedWriter().use { equivalence ->
            build.child("containment-report.md").bufferedWriter().use { containment ->
                for (testPath in tests.alongsideNonIdenticalTests) {
                    checkTest(testPath, equivalence, similarity, containment)
                }
            }
        }
    }

    status.done("Found ${alongsideNonEquivalentTests.size} non-equivalences among alongside tests (~${alongsideNonEquivalentTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTests.size} tests are equivalent (~${(tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonSimilarTests.size} non-similarities among alongside tests (~${alongsideNonSimilarTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTests.size} tests are similar (~${(tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonContainedTests.size} non-containment-s among alongside tests (~${alongsideNonContainedTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonContainedTests.size} tests are similar (~${(tests.alongsideNonIdenticalTests.size - alongsideNonContainedTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonContainedTestsWithIssues.size} non-contained tests referencing some KT-XXXX tickets, but possibly not the ones describing the differences!")

    build.child("similarity-diagnostics-stats.md").renderDiagnosticsStatistics(similarityStatistics)
    build.child("containment-diagnostics-stats.md").renderDiagnosticsStatistics(containmentStatistics)

    build.child("k2-unimplemented-diagnostics.md").writer().use { writer ->
        val missingDiagnostics = containmentStatistics.disappearedDiagnosticToFilesCount.filterKeys { it !in k2KnownErrors }
        val (withKnownIssues, newDiagnostics) = missingDiagnostics.entries.partition { it.key in knownMissingDiagnostics }

        printDiagnosticsStatistics(
            "These diagnostics are present in K1 files, but are missing in K2 altogether:",
            newDiagnostics.associate { it.key to it.value },
            writer,
        )

        printDiagnosticsStatistics(
            diagnostics = withKnownIssues.associate { it.key to it.value },
            writer = writer,
        )
    }

    val a = 10 + 1
    println("")
}
