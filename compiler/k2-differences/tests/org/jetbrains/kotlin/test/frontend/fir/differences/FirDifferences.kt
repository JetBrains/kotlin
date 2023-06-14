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
//    ),
//    listOf(
        "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
        "UNRESOLVED_REFERENCE",
        "UNRESOLVED_MEMBER",
        "UNRESOLVED_IMPORT",
        "UNRESOLVED_LABEL",
        "NO_THIS",
        "TOO_MANY_ARGUMENTS",
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
    ),
    listOf("DELEGATE_SPECIAL_FUNCTION_MISSING", "DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE"),
    listOf(
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR",
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM",
    ),
    listOf(
        "ASSIGNMENT_IN_EXPRESSION_CONTEXT",
        "EXPRESSION_EXPECTED",
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

fun ParsedCodeMetaInfo.replaceOffsetsWithLineNumbersWithin(lineStartOffsets: IntArray): ParsedCodeMetaInfo {
    val lineIndex = lineStartOffsets.getLineNumberForOffset(start)
    return ParsedCodeMetaInfo(lineIndex + 1, lineIndex + 1, attributes, tag, description)
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

fun EquivalenceTestResult.ifTests(
    testFile: File,
    areNonEquivalent: (EquivalenceTestResult) -> Unit,
    containObsoleteFeatures: () -> Unit,
    checkJvmDiagnosticButAreNotJvmTests: (List<String>) -> Unit,
    collectObsoleteFeatures: () -> List<String>,
) {
    val areEquivalent = significantK1MetaInfo.isEmpty() && significantK2MetaInfo.isEmpty()

    if (!areEquivalent) {
        val obsoleteFeatures = collectObsoleteFeatures()

        if (obsoleteFeatures.isNotEmpty()) {
            containObsoleteFeatures()
            return
        }

        if (
            !"testsWithJvmBackend".substring.matches(testFile.path) &&
            significantK1MetaInfo.isNotEmpty() &&
            significantK1MetaInfo.all { it.tag in k1JvmDefinitelyNonErrors }
        ) {
            checkJvmDiagnosticButAreNotJvmTests(significantK1MetaInfo.map { it.tag })
            return
        }

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

fun collectObsoleteDisabledLanguageFeatures(
    text: String,
    filePath: String,
): List<String> {
    val languageString = """// ?!?LANGUAGE:\s*(.*)""".toRegex().find(text)?.groupValues
        ?: return emptyList()

    return languageString[1].split("""\s+""".toRegex()).filter { featureString ->
        val matcher = LANGUAGE_FEATURE_PATTERN.matcher(featureString)
        if (!matcher.find()) {
            error("Invalid language feature pattern: $featureString (of ${languageString.first()} in $filePath)")
        }
        if (matcher.group(1) != "-") {
            return@filter false
        }
        val name = matcher.group(2)
        val feature = LanguageFeature.fromString(name) ?: error("No such language feature found: $name")
        feature.sinceVersion?.let { it <= LanguageVersion.KOTLIN_2_0 } == true
    }
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
        disappearedDiagnosticToFilesCount.put(it.tag, disappearedCount + 1)
    }

    for (it in result.significantK2MetaInfo) {
        val introducedCount = introducedDiagnosticToFilesCount.getOrDefault(it.tag, 0)
        introducedDiagnosticToFilesCount.put(it.tag, introducedCount + 1)
    }
}

fun printDiagnosticsStatistics(title: String, diagnostics: Map<String, Int>, writer: Writer) {
    writer.write("$title\n\n")
    val sorted = diagnostics.entries.sortedByDescending { it.value }

    for (it in sorted) {
        writer.write("- `${it.key}`: ${it.value} files\n")
    }
}

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

    var alongsideNonEquivalentTestsCount = 0
    var alongsideNonSimilarTestsCount = 0

    val diagnosticsStatistics = DiagnosticsStatistics()

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

    fun reportObsoleteFeatures(
        writer: Writer,
        obsoleteFeatures: List<String>,
        relativeTestPath: String,
    ) {
        writer.write("The `${relativeTestPath}` tests are not really equivalent, but they disable the `$obsoleteFeatures` features which K2 is not expected to support anyway, because they become stable before 2.0.\n\n")
    }

    fun reportJvmDiagnosticsInNonJvmTest(
        writer: Writer,
        jvmErrors: List<String>,
        relativeTestPath: String,
    ) {
        writer.write("The `${relativeTestPath}` tests are not really equivalent, but the K1 file only contains JVM-specific errors (`${jvmErrors}`), and this test is not placed in the correct folder for jvm-specific tests\n\n")
    }

    fun checkTest(
        testPath: String,
        equivalence: Writer,
        similarity: Writer,
    ) {
        status.loading("Checking $testPath", probability = 0.01)
        val relativeTestPath = testPath.removePrefix(projectDirectory.path)
        val test = File(testPath)

        val k1Text = test.readText()
        val k2Text = test.analogousK2File.readText()

        val obsoleteFeatures by lazy {
            collectObsoleteDisabledLanguageFeatures(k1Text, relativeTestPath)
        }

        val allK1MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(k1Text).filter { it.isProbablyK1Error }
        val allK2MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(k2Text).filter { it.isProbablyK2Error }

        analyseEquivalencesAmong(allK1MetaInfos, allK2MetaInfos).ifTests(
            test,
            areNonEquivalent = { result ->
                alongsideNonEquivalentTestsCount++
                reportEquivalenceDifference(equivalence, result, relativeTestPath)
            },
            containObsoleteFeatures = {
                reportObsoleteFeatures(equivalence, obsoleteFeatures, relativeTestPath)
            },
            checkJvmDiagnosticButAreNotJvmTests = { jvmErrors ->
                reportJvmDiagnosticsInNonJvmTest(equivalence, jvmErrors, relativeTestPath)
            },
            collectObsoleteFeatures = { obsoleteFeatures },
        )

        val k1LineStartOffsets = clearTextFromDiagnosticMarkup(k1Text).lineStartOffsets
        val k2LineStartOffsets = clearTextFromDiagnosticMarkup(k2Text).lineStartOffsets

        val allK1LineMetaInfos = allK1MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k1LineStartOffsets) }
        val allK2LineMetaInfos = allK2MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k2LineStartOffsets) }

        analyseEquivalencesAmong(allK1LineMetaInfos, allK2LineMetaInfos).ifTests(
            test,
            areNonEquivalent = { result ->
                alongsideNonSimilarTestsCount++
                reportEquivalenceDifference(similarity, result, relativeTestPath)
                diagnosticsStatistics.recordDiagnosticsStatistics(result)
            },
            containObsoleteFeatures = {
                reportObsoleteFeatures(similarity, obsoleteFeatures, relativeTestPath)
            },
            checkJvmDiagnosticButAreNotJvmTests = { jvmErrors ->
                reportJvmDiagnosticsInNonJvmTest(similarity, jvmErrors, relativeTestPath)
            },
            collectObsoleteFeatures = { obsoleteFeatures },
        )
    }

    build.child("similarity-report.md").bufferedWriter().use { similarity ->
        build.child("equivalence-report.md").bufferedWriter().use { equivalence ->
            for (testPath in tests.alongsideNonIdenticalTests) {
                checkTest(testPath, equivalence, similarity)
            }
        }
    }

    status.done("Found $alongsideNonEquivalentTestsCount non-equivalences among alongside tests (~${alongsideNonEquivalentTestsCount.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTestsCount} tests are equivalent (~${(tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTestsCount).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found $alongsideNonSimilarTestsCount non-similarities among alongside tests (~${alongsideNonSimilarTestsCount.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTestsCount} tests are similar (~${(tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTestsCount).outOfAllAlongsideTests()}% of all alongside tests)")

    build.child("diagnostics-stats.md").bufferedWriter().use { writer ->
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

    val a = 10 + 1
    println("")
}
