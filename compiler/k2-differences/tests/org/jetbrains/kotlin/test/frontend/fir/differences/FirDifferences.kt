/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import java.io.File

val equivalentDiagnostics = listOf(
    listOf("PARCELABLE_CANT_BE_LOCAL_CLASS", "PARCELABLE_SHOULD_BE_CLASS"),
    listOf(
        "TYPE_MISMATCH",
        "ARGUMENT_TYPE_MISMATCH",
        "RETURN_TYPE_MISMATCH",
        "ASSIGNMENT_TYPE_MISMATCH",
        "INITIALIZER_TYPE_MISMATCH",
        "CONSTANT_EXPECTED_TYPE_MISMATCH",
        "UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS",
        "UPPER_BOUND_VIOLATED",
    ),
    listOf("UNRESOLVED_REFERENCE_WRONG_RECEIVER", "UNRESOLVED_REFERENCE", "UNRESOLVED_MEMBER"),
    listOf("VAL_REASSIGNMENT", "NONE_APPLICABLE"),
    listOf("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE"),
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

val ParsedCodeMetaInfo.isProbablyError get() = !tag.startsWith("DEBUG_INFO_") && tag !in k1DefinitelyNonErrors

fun hasNonIdenticalButEquivalentResults(
    alongsideNonIdenticalTest: File,
): Boolean {
    val allK1MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(alongsideNonIdenticalTest.readText())
    val allK2MetaInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(alongsideNonIdenticalTest.analogousK2File.readText())

    val erroneousK1MetaInfos = allK1MetaInfos.filter { it.isProbablyError }
    val erroneousK2MetaInfos = allK2MetaInfos.filter { it.isProbablyError }

    val (significantK1MetaInfo, significantK2MetaInfo) = when {
        allK1MetaInfos.size + allK2MetaInfos.size <= MAGIC_DIAGNOSTICS_THRESHOLD -> {
            extractSignificantMetaInfosSlow(erroneousK1MetaInfos, erroneousK2MetaInfos)
        }
        else -> {
            extractSignificantMetaInfos(erroneousK1MetaInfos, erroneousK2MetaInfos)
        }
    }

    return significantK1MetaInfo.isEmpty() && significantK2MetaInfo.isEmpty()
}

fun main() {
    val projectDirectory = File(System.getProperty("user.dir"))
    val build = projectDirectory.child("compiler").child("k2-differences").child("build")

    val tests = deserializeOrGenerate(build.child("testsStats.json")) {
        collectTestsStats(projectDirectory)
    }

    val status = StatusPrinter()

    val alongsideNonEquivalentTests = tests.alongsideNonIdenticalTests
        .filterNot {
            status.loading("Checking $it", probability = 0.01)
            hasNonIdenticalButEquivalentResults(File(it))
        }

    status.done("Found ${alongsideNonEquivalentTests.size} differences among alongside tests")
    print("")

    val a = 2 + 3
    print("")
}
