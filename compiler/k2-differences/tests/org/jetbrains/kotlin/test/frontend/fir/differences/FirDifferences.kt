/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.test.frontend.fir.differences.AdditionalTestFailureKind.COMPILE_TIME
import org.jetbrains.kotlin.test.frontend.fir.differences.AdditionalTestFailureKind.RUNTIME
import org.jetbrains.kotlin.test.util.LANGUAGE_FEATURE_PATTERN
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
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
        "JSCODE_ARGUMENT_SHOULD_BE_CONSTANT",
        "HAS_NEXT_FUNCTION_TYPE_MISMATCH",
        "CONDITION_TYPE_MISMATCH",
        "EXPECTED_TYPE_MISMATCH",
        "EXPECTED_PARAMETERS_NUMBER_MISMATCH",
        "EXPECTED_PARAMETER_TYPE_MISMATCH",
        "TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN",
        "SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED",
        "UNSUPPORTED_FEATURE",
        "EXPLICIT_BACKING_FIELDS_UNSUPPORTED",
        "UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION",
        "UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS",
        "UPPER_BOUND_VIOLATED",
        "NEW_INFERENCE_ERROR",
        "NO_VALUE_FOR_PARAMETER",
        "SUPER_CANT_BE_EXTENSION_RECEIVER",
        "ENUM_ENTRY_SHOULD_BE_INITIALIZED",
        "NO_SET_METHOD",
        "SETTER_PROJECTED_OUT",
        "TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS",
        "AMBIGUOUS_ANNOTATION_ARGUMENT",
        "INTEGER_OPERATOR_RESOLVE_WILL_CHANGE",
        "INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR",
//    ),
//    listOf(
        "CONTRACT_NOT_ALLOWED",
        "UNSUPPORTED",
        "UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS",
        "INAPPLICABLE_OPERATOR_MODIFIER",
        "USAGE_IS_NOT_INLINABLE",
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
        "DEPRECATION",
        "FUNCTION_CALL_EXPECTED",
        "OVERLOAD_RESOLUTION_AMBIGUITY",
        "NO_COMPANION_OBJECT",
        "NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE",
        "RESOLUTION_TO_CLASSIFIER",
        "EXPECT_CLASS_AS_FUNCTION",
        "INNER_CLASS_CONSTRUCTOR_NO_RECEIVER",
        "FUNCTION_EXPECTED",
        "INTERFACE_AS_FUNCTION",
        "ENUM_ENTRY_AS_TYPE",
        "RECURSIVE_TYPEALIAS_EXPANSION",
        "MISSING_DEPENDENCY_CLASS",
        "CANNOT_INFER_PARAMETER_TYPE",
        "VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION",
        "COMPONENT_FUNCTION_MISSING",
        "COMPONENT_FUNCTION_AMBIGUITY",
        "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER",
        "FORBIDDEN_BINARY_MOD_AS_REM",
        "API_NOT_AVAILABLE",
        "NO_CONSTRUCTOR",
        "ANNOTATION_ARGUMENT_MUST_BE_CONST",
        "OPT_IN_USAGE_ERROR",
        "EXPRESSION_EXPECTED_PACKAGE_FOUND",
        "ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE",
//    ),
//    listOf(
        "CLASS_LITERAL_LHS_NOT_A_CLASS",
        "RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS",
        "EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS",
//    ),
//    listOf(
        "RETURN_NOT_ALLOWED",
        "NOT_A_FUNCTION_LABEL",
//    )
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
        "OUTER_CLASS_ARGUMENTS_REQUIRED",
        "TYPE_ARGUMENTS_NOT_ALLOWED",
//    ),
//    listOf(
        "MIXING_NAMED_AND_POSITIONED_ARGUMENTS",
        "POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION",
//    )
//    listOf(
        "VAL_REASSIGNMENT",
        "CAPTURED_MEMBER_VAL_INITIALIZATION",
        "NONE_APPLICABLE",
        "CAPTURED_VAL_INITIALIZATION",
        "VARIABLE_EXPECTED",
        "INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING",
        "INITIALIZATION_BEFORE_DECLARATION",
//    ),
//    listOf(
        "SMARTCAST_IMPOSSIBLE",
        "ITERATOR_ON_NULLABLE",
        "UNSAFE_CALL",
        "UNSAFE_IMPLICIT_INVOKE_CALL",
//    ),
//    listOf(
        "ASSIGNMENT_IN_EXPRESSION_CONTEXT",
        "EXPRESSION_EXPECTED",
        "EXPRESSION_REQUIRED",
//    ),
//    listOf(
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR",
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM",
        "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR",
    ),
    listOf(
        "DELEGATE_SPECIAL_FUNCTION_MISSING",
        "DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE",
        "DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH",
    ),
    listOf(
        "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
        "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
        "ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY",
        "MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED",
    ),
    listOf(
        "NO_ACTUAL_FOR_EXPECT",
        "INCOMPATIBLE_MATCHING",
    ),
    listOf(
        "UNINITIALIZED_VARIABLE",
        "UNINITIALIZED_ENUM_COMPANION",
    ),
    listOf(
        "EQUALITY_NOT_APPLICABLE",
        "FORBIDDEN_IDENTITY_EQUALS",
    ),
    listOf(
        "NON_VARARG_SPREAD_ERROR", // a deprecation error in K1, a normal error in K2
        "NON_VARARG_SPREAD",
    ),
    listOf(
        "MODIFIER_LIST_NOT_ALLOWED",
        "REPEATED_ANNOTATION",
    ),
    listOf(
        "PROPERTY_TYPE_MISMATCH_ON_INHERITANCE",
        "PROPERTY_TYPE_MISMATCH_BY_DELEGATION",
    ),
    listOf(
        "PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE_ERROR",
        "PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE",
    ),
    listOf(
        "SUPER_CALL_FROM_PUBLIC_INLINE_ERROR",
        "SUPER_CALL_FROM_PUBLIC_INLINE",
    ),
    listOf(
        "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
        "NOTHING_TO_OVERRIDE",
    ),
    listOf(
        "CYCLIC_INHERITANCE_HIERARCHY",
        "EXPOSED_SUPER_INTERFACE",
    ),
    listOf(
        "INACCESSIBLE_OUTER_CLASS_EXPRESSION",
        "EXPLICIT_DELEGATION_CALL_REQUIRED",
    ),
    listOf(
        "NOT_A_LOOP_LABEL",
        "BREAK_OR_CONTINUE_OUTSIDE_A_LOOP",
    ),
    listOf(
        "NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION",
        "NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY",
    ),
    listOf(
        "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION",
        "CONST_VAL_WITH_NON_CONST_INITIALIZER",
    ),
    listOf(
        "SEALED_SUPERTYPE",
        "SEALED_SUPERTYPE_IN_LOCAL_CLASS",
    ),
    listOf(
        "NESTED_CLASS_NOT_ALLOWED",
        "LOCAL_OBJECT_NOT_ALLOWED",
        "LOCAL_INTERFACE_NOT_ALLOWED",
        "WRONG_MODIFIER_TARGET",
    ),
    listOf(
        "RETURN_TYPE_MISMATCH_ON_INHERITANCE",
        "RETURN_TYPE_MISMATCH_BY_DELEGATION",
    ),
    listOf(
        "CANNOT_CHANGE_ACCESS_PRIVILEGE",
        "CANNOT_INFER_VISIBILITY",
    ),
    listOf(
        "ANNOTATION_IN_WHERE_CLAUSE_ERROR",
        "ANNOTATION_IN_WHERE_CLAUSE_WARNING",
    ),
    listOf(
        "DATA_CLASS_OVERRIDE_DEFAULT_VALUES",
        "DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR"
    ),
    listOf(
        "TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE",
        "TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS",
    ),
    listOf(
        "ACTUAL_WITHOUT_EXPECT",
        "NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS",
    ),
    listOf(
        "PACKAGE_OR_CLASSIFIER_REDECLARATION",
        "ACTUAL_MISSING",
        "AMBIGUOUS_ACTUALS",
        "IMPLICIT_JVM_ACTUALIZATION",
    ),
)

val k2SpecificLanguageFeatures = listOf(
    LanguageFeature.ReportErrorsForComparisonOperators,
)

val projectDirectory = File(System.getProperty("user.dir"))
val build = projectDirectory.child("compiler").child("k2-differences").child("build")

object PublishableArtifacts {
    val allArtifacts = mutableListOf<File>()

    private inline fun publishable(block: () -> File) = block().also(allArtifacts::add)

    val similarityReport = publishable {
        build.child("similarity-report.md")
    }
    val equivalenceReport = publishable {
        build.child("equivalence-report.md")
    }
    val containmentReport = publishable {
        build.child("containment-report.md")
    }

    val similarityDiagnosticsStats = publishable {
        build.child("similarity-diagnostics-stats.csv")
    }
    val containmentDiagnosticsStats = publishable {
        build.child("containment-diagnostics-stats.csv")
    }

    val k2UnimplementedDiagnostics = publishable {
        build.child("k2-unimplemented-diagnostics.md")
    }

    val containmentRedToPureGreen = publishable {
        build.child("containment-red-to-pure-green.md")
    }
    val containmentPureGreenToRed = publishable {
        build.child("containment-pure-green-to-red.md")
    }
}

val status = StatusPrinter()

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
): Pair<MutableList<ParsedCodeMetaInfo>, MutableList<ParsedCodeMetaInfo>> {
    val (k1MetaInfos, k2MetaInfos, commonMetaInfos) = extractCommonMetaInfosSlow(erroneousK1MetaInfos, erroneousK2MetaInfos)

    val significantK1MetaInfo = k1MetaInfos.filterTo(mutableListOf()) { tagInK1 ->
        commonMetaInfos.none { it.isOnSamePositionAs(tagInK1) } && tagInK1.tag !in k1WarningsMatchingK2Errors
    }
    val significantK2MetaInfo = k2MetaInfos.filterTo(mutableListOf()) { tagInK2 ->
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
): Pair<MutableList<ParsedCodeMetaInfo>, MutableList<ParsedCodeMetaInfo>> {
    val (k1MetaInfos, k2MetaInfos, commonMetaInfos) = extractCommonMetaInfos(erroneousK1MetaInfos, erroneousK2MetaInfos)

    val significantK1MetaInfo = k1MetaInfos.filterNotTo(mutableListOf()) { tagInK1 ->
        commonMetaInfos.hasDiagnosticsAt(tagInK1.start, tagInK1.end) || tagInK1.tag in k1WarningsMatchingK2Errors
    }
    val significantK2MetaInfo = k2MetaInfos.filterNotTo(mutableListOf()) { tagInK2 ->
        commonMetaInfos.hasDiagnosticsAt(tagInK2.start, tagInK2.end)
    }

    return significantK1MetaInfo to significantK2MetaInfo
}

val MAGIC_DIAGNOSTICS_THRESHOLD = 80

// Ones that are replaced by errors in K2.
// Such warnings are ignored in a "lazy fashion":
// They are filtered only after we know they have
// no matches in K2. This means, if they do, then
// the matching K2 error will be eliminated by
// the algorithm.
val k1WarningsMatchingK2Errors = mapOf(
    "INTEGER_OPERATOR_RESOLVE_WILL_CHANGE" to IssueInfo("25-2791259", 38895),
//    "ANNOTATION_IN_WHERE_CLAUSE_WARNING" to IssueInfo("25-3281567", 46483),
)

val commonWarningsToBeTreatedLikeErrors = setOf(
    // The majority of tests check DEPRECATION, so
    // DEPRECATION_ERROR stays mostly uncovered
    "DEPRECATION",
)
val k1WarningsToBeTreatedLikeErrors = commonWarningsToBeTreatedLikeErrors
val k2WarningsToBeTreatedLikeErrors = commonWarningsToBeTreatedLikeErrors

val k1DefinitelyNonErrors = collectAllK1NonErrors() - k1WarningsMatchingK2Errors - k1WarningsToBeTreatedLikeErrors
val k2DefinitelyNonErrors = collectAllK2NonErrors() - k2WarningsToBeTreatedLikeErrors

val k1JvmDefinitelyNonErrors = collectFromFieldsOf(ErrorsJvm::class, instance = null, collector = ::getErrorFromFieldValue)

val k2KnownErrors = collectAllK2Errors() + k2WarningsToBeTreatedLikeErrors

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
    val startingLineOffset: Int,
    val endingLineOffset: Int,
) : ParsedCodeMetaInfo(start, end, attributes, tag, description)

val ParsedCodeMetaInfo.shiftedStart
    get() = when {
        this is ModifiedParsedCodeMetaInfo -> startingLineOffset
        else -> start
    }

val ParsedCodeMetaInfo.shiftedEnd
    get() = when {
        this is ModifiedParsedCodeMetaInfo -> endingLineOffset
        else -> start
    }

fun ParsedCodeMetaInfo.replaceOffsetsWithLineNumbersWithin(lineStartOffsets: IntArray): ParsedCodeMetaInfo {
    val lineIndex = lineStartOffsets.getLineNumberForOffset(start)
    return ParsedCodeMetaInfo(lineIndex + 1, lineIndex + 1, attributes, tag, description)
}

fun ParsedCodeMetaInfo.shiftOffsetsWithin(lineStartOffsets: IntArray, totalTextLength: Int): ParsedCodeMetaInfo {
    val shiftedStart = lineStartOffsets[lineStartOffsets.getLineNumberForOffset(start) - 1]
    val shiftedEnd = lineStartOffsets.getOrNull(lineStartOffsets.getLineNumberForOffset(end)) ?: totalTextLength
    return ModifiedParsedCodeMetaInfo(start, end, attributes, tag, description, shiftedStart, shiftedEnd)
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
    val isK1PureGreen: Boolean,
    val isK2PureGreen: Boolean,
    val significantK1MetaInfo: MutableList<ParsedCodeMetaInfo> = mutableListOf(),
    val significantK2MetaInfo: MutableList<ParsedCodeMetaInfo> = mutableListOf(),
)

fun reportEquivalenceDifference(
    writer: Appendable,
    result: EquivalenceTestResult,
    relativeTestPath: String,
) {
    writer.append("The `${relativeTestPath}` test:\n\n")

    for (it in result.significantK1MetaInfo) {
        writer.append("- `#potential-feature`: `${it.tag}` was in K1 at `(${it.start}..${it.end})`, but disappeared\n")
    }

    for (it in result.significantK2MetaInfo) {
        writer.append("- `#potential-breaking-change`: `${it.tag}` was introduced in K2 at `(${it.start}..${it.end})`\n")
    }

    writer.append("\n")
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

    return EquivalenceTestResult(allK1MetaInfos.isEmpty(), allK2MetaInfos.isEmpty(), significantK1MetaInfo, significantK2MetaInfo)
}

fun analyseEquivalencesAsHierarchyAmong(
    erroneousK1MetaInfos: List<ParsedCodeMetaInfo>,
    erroneousK2MetaInfos: List<ParsedCodeMetaInfo>,
): EquivalenceTestResult {
    val allK1MetaInfos = erroneousK1MetaInfos.toMetaInfosHierarchySet()
    val allK2MetaInfos = erroneousK2MetaInfos.toMetaInfosHierarchySet()

    val (commonK1MetaInfoList, uniqueK1MetaInfos) = erroneousK1MetaInfos.partition {
        allK2MetaInfos.hasOverlappingEquivalentOf(it)
    }
    val (commonK2MetaInfoList, uniqueK2MetaInfos) = erroneousK2MetaInfos.partition {
        allK1MetaInfos.hasOverlappingEquivalentOf(it)
    }

    val commonK1MetaInfos = commonK1MetaInfoList.toMetaInfosHierarchySet()
    val commonK2MetaInfos = commonK2MetaInfoList.toMetaInfosHierarchySet()

    val significantK1MetaInfo = uniqueK1MetaInfos.filterNotTo(mutableListOf()) {
        commonK1MetaInfos.overlapsWith(it) || it.tag in k1WarningsMatchingK2Errors
    }
    val significantK2MetaInfo = uniqueK2MetaInfos.filterNotTo(mutableListOf()) {
        commonK2MetaInfos.overlapsWith(it)
    }

    return EquivalenceTestResult(allK1MetaInfos.isEmpty(), allK2MetaInfos.isEmpty(), significantK1MetaInfo, significantK2MetaInfo)
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

    status.doneSilently("$missingCommentsCount fir files had missing TEST SPEC comments")
}

fun fixStupidEmptyLines(
    alongsideNonIdenticalTests: List<String>,
) {
    val missingCommentsCount = alongsideNonIdenticalTests.count {
        status.loading("Checking stupid empty lines in $it", probability = 0.001)
        val k1FirstLine = File(it).readLines().firstOrNull() ?: return@count false
        val k2File = File(it).analogousK2File
        val k2Lines = k2File.readLines()
        val k2FirstLine = k2File.readLines().firstOrNull() ?: return@count false

        if (k1FirstLine.isNotBlank() && k2FirstLine.isBlank()) {
            status.loading("Removing the stupid empty line in $it")

            k2File.writeText(
                k2Lines.drop(1).joinToString(System.lineSeparator())
            )

            true
        } else {
            false
        }
    }

    status.doneSilently("$missingCommentsCount fir files had stupid empty lines")
}

typealias DiagnosticsStatistics = MutableMap<String, MutableMap<File, EquivalenceTestResult>>

fun DiagnosticsStatistics.recordDiagnosticsStatistics(test: File, result: EquivalenceTestResult) {
    for (it in result.significantK1MetaInfo) {
        getOrPut(it.tag) { mutableMapOf() }.getOrPut(test) {
            EquivalenceTestResult(result.isK1PureGreen, result.isK2PureGreen)
        }.significantK1MetaInfo.add(it)
    }

    for (it in result.significantK2MetaInfo) {
        getOrPut(it.tag) { mutableMapOf() }.getOrPut(test) {
            EquivalenceTestResult(result.isK1PureGreen, result.isK2PureGreen)
        }.significantK2MetaInfo.add(it)
    }
}

fun logPossibleEquivalences(result: EquivalenceTestResult, writer: Writer) {
    val positionToK1Diagnostics = mutableMapOf<Pair<Int, Int>, MutableList<String>>()

    for (it in result.significantK1MetaInfo) {
        positionToK1Diagnostics.getOrPut(it.start to it.end) { mutableListOf() }.add(it.tag)
    }

    val matchingDiagnostics = mutableMapOf<Pair<Int, Int>, Pair<MutableList<String>, MutableList<String>>>()

    for (it in result.significantK2MetaInfo) {
        val k1Diagnostics = positionToK1Diagnostics[it.start to it.end] ?: continue
        val (_, k2Diagnostics) = matchingDiagnostics.getOrPut(it.start to it.end) { k1Diagnostics to mutableListOf() }
        k2Diagnostics.add(it.tag)
    }

    for ((position, diagnostics) in matchingDiagnostics) {
        val (k1Diagnostics, k2Diagnostics) = diagnostics
        writer.write("At `(${position.first}..${position.second})` in the K1 file there are `${k1Diagnostics}`, and in the K2 file there are `${k2Diagnostics}`. Maybe some of them are equivalent?\n\n")
    }
}

fun printSimpleDiagnosticsStatistics(
    title: String? = null,
    diagnostics: Map<String, Set<File>>,
    writer: Writer,
    knownIssuesForDiagnostics: Map<String, IssueInfo> = emptyMap(),
) {
    if (title != null) {
        writer.write("$title\n\n")
    }

    val sorted = diagnostics.entries.sortedByDescending { it.value.size }

    for (it in sorted) {
        writer.write("- `${it.key}`: ${it.value.size} files")

        val knownIssue = knownIssuesForDiagnostics[it.key]

        if (knownIssue != null) {
            val ticket = knownIssue.numberInProject
            writer.write(". The corresponding issue is [KT-$ticket](https://youtrack.jetbrains.com/issue/KT-$ticket)\n")
        } else {
            writer.write("\n")
        }
    }
}

val statsColumns = listOf(
    "Diagnostic",
    "Red->Green", // "# of files it disappeared from",
    "Green->Red", // "# of files where it's introduced into",
    "Red->Pure Green", // "# of files it disappeared from that are now pure-green",
    "Pure Green->Red", // "# of pure-green files where it's introduced into",
    "Dis.", // "Disappearance issue",
    "Int.", // "Introduction issue",
    "Mis.", // "Missing issue",
)

private fun List<*>.toCsvColumns() = joinToString(",") { "\"$it\"" }

private fun List<*>.toCsvLine() = toCsvColumns() + "\n"

fun printCsvDiagnosticsStatistics(
    diagnosticsStatistics: DiagnosticsStatistics,
    writer: Writer,
) {
    writer.write(statsColumns.toCsvLine())

    val sorted = diagnosticsStatistics.entries.sortedByDescending { it.value.size }

    for ((diagnostic, filesToStats) in sorted) {
        val disappearances = filesToStats.filter { it.value.significantK1MetaInfo.isNotEmpty() }
        val introductions = filesToStats.filter { it.value.significantK2MetaInfo.isNotEmpty() }

        val pureDisappearances = disappearances.filter { it.value.isK2PureGreen }
        val pureIntroductions = introductions.filter { it.value.isK1PureGreen }

        val disappearanceIssue = knownDisappearedDiagnostics[diagnostic]
        val introductionIssue = knownIntroducedDiagnostics[diagnostic]
        val missingIssue = knownMissingDiagnostics[diagnostic]

        writer.write(
            listOf(
                diagnostic, disappearances.size, introductions.size,
                pureDisappearances.size, pureIntroductions.size,
                disappearanceIssue?.ktNumber ?: "--",
                introductionIssue?.ktNumber ?: "--",
                missingIssue?.ktNumber ?: "--",
            ).toCsvLine()
        )
    }
}

fun DiagnosticsStatistics.extractDisappearances() =
    mapValues { (_, filesToStats) ->
        filesToStats.filter { it.value.significantK1MetaInfo.isNotEmpty() }.keys
    }.filterValues {
        it.isNotEmpty()
    }

fun DiagnosticsStatistics.extractIntroductions() =
    mapValues { (_, filesToStats) ->
        filesToStats.filter { it.value.significantK2MetaInfo.isNotEmpty() }.keys
    }.filterValues {
        it.isNotEmpty()
    }

const val MOST_COMMON_REASONS_OF_BREAKING_CHANGES = "Most common reasons of breaking changes"

val KT_ISSUE_PATTERN = """KT-?\d+""".toRegex(RegexOption.IGNORE_CASE)

fun recordCandidateForBoxTestIfNeeded(
    test: File,
    result: EquivalenceTestResult,
    shouldCheckManually: Boolean,
    candidatesForAdditionalBoxTests: MutableList<File>,
    candidatesForManualChecking: MutableMap<File, Set<String>>,
) {
    val diagnosticsToCheckViaBoxTest = result.significantK1MetaInfo.filter {
        it.tag in knownMissingDiagnostics && it.tag !in obsoleteIssues
    }

    if (diagnosticsToCheckViaBoxTest.isEmpty()) {
        return
    }

    if (shouldCheckManually) {
        candidatesForManualChecking[test] = diagnosticsToCheckViaBoxTest.map { it.tag }.toSet()
        return
    }

    candidatesForAdditionalBoxTests.add(test)
}

enum class AdditionalTestFailureKind {
    COMPILE_TIME, RUNTIME,
}

val knownFailingAdditionalBoxTests = mapOf(
    "js/js.translator/testData/box/k2DifferencesChecks/propertyDelegateBy.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/badAssignment.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/error.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/deleteOperation.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/wrongCallToNonModule.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/wrongCallToModule.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/dualModuleFromUmd.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/overrideOverloadedNativeFunction.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/dynamicCastTarget.kt" to COMPILE_TIME,
    "js/js.translator/testData/box/k2DifferencesChecks/illegalName.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/coercionToUnitWithNothingType.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/innerTypeAliasAsType.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/dataClassExplicitlyOverridingCopyNoDefaults.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/clash.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperClass.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/12.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/13.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/kt45796.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/kt53639.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/inheritanceAmbiguity.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/multiLambdaRestriction.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperFunInterface_1.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperFunInterface_2.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/kt51062Error.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypes.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperinterface_1.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperinterface_2.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/recursiveLambda.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/noContextReceiversOnValueClasses.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/1.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/2.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/4.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/5.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/6.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperinterface.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/inheritanceAmbiguity2.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/inheritanceAmbiguity3.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/inheritanceAmbiguity4.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/pureKotlin.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/multipleInheritedDefaults.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/nestedAndTopLevelClassClash.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/nestedClassClash.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperClass_1.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperClass_2.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/constructorInHeaderEnum.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypes_1.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypes_2.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/returnTypeNothingShouldBeSpecifiedExplicitly.kt" to RUNTIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/mixingSuspendAndNonSuspendSupertypesThruSuperFunInterface.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/casesWithTwoTypeParameters.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/recursiveFun.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/dataClassExplicitlyOverridingCopyWithDefaults.kt" to COMPILE_TIME,
    "compiler/testData/codegen/box/k2DifferencesChecks/protectedSuperCall.kt" to COMPILE_TIME,
).map {
    val file = projectDirectory.child(it.key)
    val newKey = """// ORIGINAL: (.*)""".toRegex().find(file.readText())?.groupValues?.get(1)
        ?: error("No link to the original file found in $it")
    newKey to it.value
}.toMap()

fun generateAdditionalBoxTestsAndLogManuals(
    candidatesForAdditionalBoxTests: List<File>,
    candidatesForManualChecking: Map<File, Set<String>>,
) {
    val nextIndexAfter = mutableMapOf<String, Int>()

    fun nextNameFor(baseName: String): String {
        val index = nextIndexAfter[baseName] ?: 0
        val name = if (index > 0) "${baseName}_$index" else baseName
        return name.also {
            nextIndexAfter[baseName] = index + 1
        }
    }

    for (it in candidatesForAdditionalBoxTests) {
        val boxTestsDirectory = when {
            "compiler/testData/diagnostics/testsWithJsStdLib" in it.path -> "js/js.translator/testData/box"
            "/plugins/" in it.path -> {
                val plugin = extractPluginNameFrom(it.path)
                "plugins/$plugin/testData/box"
            }
            else -> "compiler/testData/codegen/box"
        }

        val k2DifferencesChecks = projectDirectory.child(boxTestsDirectory).child("k2DifferencesChecks")
            .also { it.mkdirs() }
        val name = nextNameFor(it.name.split(".").first())
        val additionalTest = k2DifferencesChecks.child("$name.kt")
        val relativePath = it.path.removePrefix(projectDirectory.path)
        val textPossiblyWithWarnings = it.readText().replace("// FIR_DUMP", "")
        val text = clearTextFromDiagnosticMarkup(textPossiblyWithWarnings)

        val textWithBox = when {
            "fun box(): String?" in text -> text.replace("fun box(): String?", "fun vox(): String?") +
                    "\n\nfun box() = vox() ?: \"FAIL\"\n"
            "fun box(): String" in text -> text
            "fun box()" in text -> text.replace("fun box()", "fun vox()") +
                    "\n\nfun box() = \"OK\".also { vox() }\n"
            "fun test()" in text -> "$text\n\nfun box() = \"OK\".also { test() }\n"
            "fun main()" in text -> "$text\n\nfun box() = \"OK\".also { main() }\n"
            "fun foo()" in text -> "$text\n\nfun box() = \"OK\".also { foo() }\n"
            else -> "$text\n\nfun box() = \"OK\"\n"
        }

        val originalReference = "// ORIGINAL: $relativePath\n"
        val stdlibDirective = "// WITH_STDLIB\n"
        val stdlib = if (stdlibDirective in text) "" else stdlibDirective
        additionalTest.writeText(originalReference + stdlib + textWithBox)
        status.loading("Regenerating $relativePath")
    }

    status.doneSilently("Additional box tests generated")

    if (candidatesForManualChecking.isEmpty()) {
        return
    }

    status.doneSilently("The following tests contain other errors, so they have to be checked manually")

    for ((index, it) in candidatesForManualChecking.entries.withIndex()) {
        val (file, diagnostics) = it
        println("- $index: ${file.path.removePrefix(projectDirectory.path)}")
        println("  - ${diagnostics.joinToString()}")
    }
}

val File.analogousK2RelativePath get() = analogousK2File.path.removePrefix(projectDirectory.path)

const val WHEN_TURNED_INTO_BOX_TEST = "When turned into a box test"

fun buildFailingPassingAdditionalTestsStatisticsMessage(
    failingBoxes: List<File>,
    passingBoxes: List<File>,
) = buildString {
    append("Some of the K2 tests that have this diagnostic missing, when executed as box-tests, lead to #k2-compiler-crash.\n\n")
    append("$WHEN_TURNED_INTO_BOX_TEST, the following tests fail:\n\n")

    for (it in failingBoxes) {
        append("- ${it.analogousK2RelativePath}\n")
    }

    append("\n")
    append("$WHEN_TURNED_INTO_BOX_TEST, the following tests pass:\n\n")

    for (it in passingBoxes) {
        append("- ${it.analogousK2RelativePath}\n")
    }
}

fun updateKnownIssuesDescriptions(statistics: DiagnosticsStatistics) {
    for ((diagnostic, filesToEntries) in statistics) {
        knownMissingDiagnostics[diagnostic]?.let { knownIssue ->
            updateIssueDescription(knownIssue, filesToEntries)
        }

        knownDisappearedDiagnostics[diagnostic]?.let { knownIssue ->
            val disappearancesOnly = filesToEntries
                .filterValues { it.significantK1MetaInfo.isNotEmpty() }
                .mapValues {
                    EquivalenceTestResult(
                        it.value.isK1PureGreen, it.value.isK2PureGreen,
                        significantK1MetaInfo = it.value.significantK1MetaInfo,
                    )
                }
            updateIssueDescription(knownIssue, disappearancesOnly)
        }

        knownIntroducedDiagnostics[diagnostic]?.let { knownIssue ->
            val introductionsOnly = filesToEntries
                .filterValues { it.significantK2MetaInfo.isNotEmpty() }
                .mapValues {
                    EquivalenceTestResult(
                        it.value.isK1PureGreen, it.value.isK2PureGreen,
                        significantK2MetaInfo = it.value.significantK2MetaInfo,
                    )
                }
            updateIssueDescription(knownIssue, introductionsOnly)
        }
    }

    knownMissingDiagnostics.filterKeys { it !in statistics }.forEach { (_, knownIssue) ->
        updateIssueDescription(knownIssue, emptyMap())
    }

    knownDisappearedDiagnostics.filterKeys { it !in statistics }.forEach { (_, knownIssue) ->
        updateIssueDescription(knownIssue, emptyMap())
    }

    knownIntroducedDiagnostics.filterKeys { it !in statistics }.forEach { (_, knownIssue) ->
        updateIssueDescription(knownIssue, emptyMap())
    }
}

fun updateIssueDescription(
    knownIssue: IssueInfo,
    filesToEntries: Map<File, EquivalenceTestResult>,
) {
    try {
        val resolvedResult = getJson(
            "https://youtrack.jetbrains.com/api/issues/${knownIssue.id}?fields=resolved",
            API_HEADERS,
        ).also(::println)

        if ("\"resolved\":null" !in resolvedResult) {
            return
        }

        postJson(
            "https://youtrack.jetbrains.com/api/issues/${knownIssue.id}?fields=id,resolved",
            API_HEADERS,
            mapOf(
                "description" to buildDiagnosticStatisticsIssueDescription(filesToEntries),
            ),
        ).also(::println)
    } catch (e: IOException) {
        println(e)
    }
}

fun buildDiagnosticStatisticsIssueDescription(
    filesToEntries: Map<File, EquivalenceTestResult>,
) = buildString {
    append("*Don't edit this issue description, it will be regenerated automatically.*\n\n")

    if (filesToEntries.isEmpty()) {
        append("According to the reports in the parent KT-58630 issue, this diagnostic no longer causes any significant differences. This issue should probably be closed.")
        return@buildString
    }

    append("This diagnostic is backed up by ${filesToEntries.size} tests. See the reports in the parent KT-58630 issue for more details.\n\n")

    for ((file, entry) in filesToEntries) {
        reportEquivalenceDifference(this, entry, file.path.removePrefix(projectDirectory.path))
    }
}

val mainCompletenessIssueId = IssueInfo("25-4446335", 58630)

fun publishReports() {
    val attachmentsJson = getJson(
        "https://youtrack.jetbrains.com/api/issues/${mainCompletenessIssueId.id}?fields=attachments(id,name,url)",
        API_HEADERS,
    ).also(::println)

    val existingAttachments = "\"id\":\"([^\"]*)\"".toRegex()
        .findAll(attachmentsJson)
        .map { it.groupValues.last() }
        .toList()

    for (attachment in existingAttachments) {
        deleteJson(
            "https://youtrack.jetbrains.com/api/issues/${mainCompletenessIssueId.id}/attachments/$attachment",
            API_HEADERS,
        ).also(::println)
    }

    uploadFiles(
        "https://youtrack.jetbrains.com/api/issues/${mainCompletenessIssueId.id}/attachments?fields=id,url,name",
        PublishableArtifacts.allArtifacts,
    ).also(::println)
}

fun updateMainCompletenessIssue() {
    publishReports()

    postJson(
        "https://youtrack.jetbrains.com/api/issues/${mainCompletenessIssueId.id}/comments?fields=id,author(name),text",
        API_HEADERS,
        mapOf(
            "text" to status.outputCopy,
        ),
    ).also(::println)
}

fun doNonLocalThings(containmentStatistics: DiagnosticsStatistics) {
    val diagnosticsWithNoIssuesCount = containmentStatistics.keys.count {
        it !in knownMissingDiagnostics && it !in knownDisappearedDiagnostics && it !in knownIntroducedDiagnostics
    }

    status.doneSilently("There are $diagnosticsWithNoIssuesCount diagnostics with no issues")

    generateMissingIssues(containmentStatistics)
//    updateMissingDiagnosticsTags(containmentStatistics)
    updateKnownIssuesDescriptions(containmentStatistics)
    updateMainCompletenessIssue()
}

inline fun <reified T> JsonElement.cast(): T = this as? T ?: error("Expected ${T::class.simpleName}, but was: $this")

inline fun <reified T> JsonObject.getChildAs(key: String): T = this[key] as? T ?: error("Expected ${T::class.simpleName}, but was: ${this[key]}")

inline fun <reified T, R> JsonArray.mapChildrenAs(transform: (T) -> R): List<R> = map {
    transform(it as? T ?: error("Expected ${T::class.simpleName}, but was: $it"))
}

fun main() {
    val tests = deserializeOrGenerate(build.child("testsStats.json")) {
        collectTestsStats(projectDirectory)
    }

    fixMissingTestSpecComments(tests.alongsideNonIdenticalTests)
    fixStupidEmptyLines(tests.alongsideNonIdenticalTests)

    fun Int.outOfAllAlongsideTests(): Int = this * 100 / (tests.alongsideNonIdenticalTests.size + tests.alongsideIdenticalTests.size)

    status.done("${tests.alongsideNonIdenticalTests.size} tests are non-identical among the total of ${tests.alongsideNonIdenticalTests.size + tests.alongsideIdenticalTests.size} alongside tests (~${tests.alongsideNonIdenticalTests.size.outOfAllAlongsideTests()}%)")

    val alongsideNonEquivalentTests = mutableListOf<File>()
    val alongsideNonSimilarTests = mutableListOf<File>()
    val alongsideNonContainedTests = mutableListOf<File>()
    val alongsideNonContainedTestsWithIssues = mutableMapOf<File, String>()

    val similarityStatistics: DiagnosticsStatistics = mutableMapOf()
    val containmentStatistics: DiagnosticsStatistics = mutableMapOf()

    val candidatesForAdditionalBoxTests = mutableListOf<File>()
    val candidatesForManualChecking = mutableMapOf<File, Set<String>>()

    fun checkTest(
        testPath: String,
        equivalence: Writer,
        similarity: Writer,
        containment: Writer,
        containmentRedToPureGreen: Writer,
        containmentPureGreenToRed: Writer,
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

        val clearedK1Text = clearTextFromDiagnosticMarkup(k1Text)
        val clearedK2Text = clearTextFromDiagnosticMarkup(k2Text)

        val k1LineStartOffsets = clearedK1Text.lineStartOffsets
        val k2LineStartOffsets = clearedK2Text.lineStartOffsets

        val allK1LineMetaInfos = allK1MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k1LineStartOffsets) }
        val allK2LineMetaInfos = allK2MetaInfos.map { it.replaceOffsetsWithLineNumbersWithin(k2LineStartOffsets) }

        val similarityAnalysisResults = analyseEquivalencesAmong(allK1LineMetaInfos, allK2LineMetaInfos)

        similarityAnalysisResults.ifTests(
            test, similarity, relativeK1TestPath, relativeK2TestPath,
            areNonEquivalent = { result ->
                alongsideNonSimilarTests.add(test)
                similarityStatistics.recordDiagnosticsStatistics(test, result)
            },
            collectObsoleteFeatures = { obsoleteFeatures },
            collectBrandNewFeatures = { brandNewFeatures },
        )

        val allK1ShiftedMetaInfos = allK1MetaInfos.map { it.shiftOffsetsWithin(k1LineStartOffsets, clearedK1Text.length) }
        val allK2ShiftedMetaInfos = allK2MetaInfos.map { it.shiftOffsetsWithin(k2LineStartOffsets, clearedK2Text.length) }

        analyseEquivalencesAsHierarchyAmong(allK1ShiftedMetaInfos, allK2ShiftedMetaInfos).ifTests(
            test, containment, relativeK1TestPath, relativeK2TestPath,
            areNonEquivalent = { result ->
                alongsideNonContainedTests.add(test)
                containmentStatistics.recordDiagnosticsStatistics(test, result)
                logPossibleEquivalences(result, containment)

                when {
                    allK1MetaInfos.isNotEmpty() && allK2MetaInfos.isEmpty() -> containmentRedToPureGreen
                    allK1MetaInfos.isEmpty() && allK2MetaInfos.isNotEmpty() -> containmentPureGreenToRed
                    else -> null
                }?.let {
                    reportEquivalenceDifference(it, result, relativeK1TestPath)
                }

                recordCandidateForBoxTestIfNeeded(
                    test.analogousK2File, result, shouldCheckManually = allK2MetaInfos.isNotEmpty() || "expect " in k2Text,
                    candidatesForAdditionalBoxTests, candidatesForManualChecking,
                )

                KT_ISSUE_PATTERN.find(k2Text)?.groupValues?.first()?.let {
                    alongsideNonContainedTestsWithIssues[test] = it
                }
            },
            collectObsoleteFeatures = { obsoleteFeatures },
            collectBrandNewFeatures = { brandNewFeatures },
        )
    }

    val similarityWriter = PublishableArtifacts.similarityReport.bufferedWriter()
    val equivalenceWriter = PublishableArtifacts.equivalenceReport.bufferedWriter()
    val containmentWriter = PublishableArtifacts.containmentReport.bufferedWriter()
    val containmentRedToPureGreenWriter = PublishableArtifacts.containmentRedToPureGreen.bufferedWriter()
    val containmentPureGreenToRedWriter = PublishableArtifacts.containmentPureGreenToRed.bufferedWriter()

    useAll(similarityWriter, equivalenceWriter, containmentWriter, containmentRedToPureGreenWriter, containmentPureGreenToRedWriter) {
        for (testPath in tests.alongsideNonIdenticalTests) {
            checkTest(
                testPath, equivalenceWriter, similarityWriter, containmentWriter,
                containmentRedToPureGreenWriter, containmentPureGreenToRedWriter,
            )
        }
    }

    status.done("Found ${alongsideNonEquivalentTests.size} non-equivalences among alongside tests (~${alongsideNonEquivalentTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTests.size} tests are equivalent (~${(tests.alongsideNonIdenticalTests.size - alongsideNonEquivalentTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonSimilarTests.size} non-similarities among alongside tests (~${alongsideNonSimilarTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTests.size} tests are similar (~${(tests.alongsideNonIdenticalTests.size - alongsideNonSimilarTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonContainedTests.size} non-containment-s among alongside tests (~${alongsideNonContainedTests.size.outOfAllAlongsideTests()}% of all alongside tests). That is ${tests.alongsideNonIdenticalTests.size - alongsideNonContainedTests.size} tests are similar (~${(tests.alongsideNonIdenticalTests.size - alongsideNonContainedTests.size).outOfAllAlongsideTests()}% of all alongside tests)")
    status.done("Found ${alongsideNonContainedTestsWithIssues.size} non-contained tests referencing some KT-XXXX tickets, but possibly not the ones describing the differences!")

    PublishableArtifacts.similarityDiagnosticsStats.bufferedWriter().use {
        printCsvDiagnosticsStatistics(similarityStatistics, it)
    }
    PublishableArtifacts.containmentDiagnosticsStats.bufferedWriter().use {
        printCsvDiagnosticsStatistics(containmentStatistics, it)
    }

    PublishableArtifacts.k2UnimplementedDiagnostics.writer().use { writer ->
        val missingDiagnostics = containmentStatistics.extractDisappearances().filterKeys { it !in k2KnownErrors }
        val (withKnownIssues, newDiagnostics) = missingDiagnostics.entries.partition { it.key in knownMissingDiagnostics }

        printSimpleDiagnosticsStatistics(
            "These diagnostics are present in K1 files, but are missing in K2 altogether:",
            newDiagnostics.associate { it.key to it.value },
            writer,
        )

        printSimpleDiagnosticsStatistics(
            title = null,
            diagnostics = withKnownIssues.associate { it.key to it.value },
            writer = writer,
            knownMissingDiagnostics,
        )
    }

    generateAdditionalBoxTestsAndLogManuals(candidatesForAdditionalBoxTests, candidatesForManualChecking)
//    doNonLocalThings(containmentStatistics)

    @Suppress("UNUSED_VARIABLE") val a = 10 + 1
    println("")
}

class Runner {
    @Test
    fun runMain() = main()
}
