/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.klib.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.CompilerSingleFileKlibResolveAllowingIrProvidersStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib

private inline fun <reified T : Any> compareElements(
        comparisonConfig: ComparisonConfig,
        elements: Map<String, Pair<T, T>>,
        crossinline comparator: (T, T) -> MetadataCompareResult
): MetadataCompareResult = elements
        .entries
        .asSequence()
        .filter { comparisonConfig.shouldCheckDeclaration(it.key) }
        .map { comparator(it.value.first, it.value.second).messageIfFail("${it.key} mismatch") }
        .toList()
        .wrap()

private class JoinedFragments(
        val classes: JoinResult<KmClass>,
        val functions: JoinResult<KmFunction>,
        val properties: JoinResult<KmProperty>,
        val typeAliases: JoinResult<KmTypeAlias>,
)

private fun processMissing(comparisonConfig: ComparisonConfig, joinResult: JoinResult<*>): MetadataCompareResult {
    val missingInFirst = joinResult.missingInFirst
            .filter(comparisonConfig::shouldCheckDeclaration)
            .map { Fail("$it missing in first fragment") }
    val missingInSecond = joinResult.missingInSecond
            .filter(comparisonConfig::shouldCheckDeclaration)
            .map { Fail("$it missing in second fragment") }
    return (missingInFirst + missingInSecond).let {
        if (it.isEmpty()) Ok else Fail(it)
    }
}

private fun MetadataCompareResult.messageIfFail(message: String): MetadataCompareResult =
        if (this is Fail) Fail(message, this) else this

private fun processMissing(
        comparisonConfig: ComparisonConfig,
        joinedFragments: JoinedFragments
): MetadataCompareResult = listOf(
        processMissing(comparisonConfig, joinedFragments.classes)
                .messageIfFail("Missing classes"),
        processMissing(comparisonConfig, joinedFragments.functions)
                .messageIfFail("Missing functions"),
        processMissing(comparisonConfig, joinedFragments.typeAliases)
                .messageIfFail("Missing type aliases"),
        processMissing(comparisonConfig, joinedFragments.properties)
                .messageIfFail("Missing properties"),
).wrap()

private data class JoinResult<T>(
        val joined: Map<String, Pair<T, T>>,
        val missingInFirst: List<String>,
        val missingInSecond: List<String>
)

private fun <T> buildJoined(e1: List<T>, e2: List<T>, key: T.() -> String): JoinResult<T> {
    val m1 = e1.associateBy { it.key() }
    val m2 = e2.associateBy { it.key() }
    val joinedKeys = e1.map(key).filter { it in m2 }.toSet()
    val joined = m1
            .filterKeys(joinedKeys::contains)
            .mapValues { (key, value) -> value to m2.getValue(key) }
    return JoinResult(
            joined,
            (m1 - joinedKeys).keys.toList(),
            (m2 - joinedKeys).keys.toList()
    )
}

/**
 * Wrapper around direct access to [fragment] that allows
 * to uniformly process all its components.
 */
private fun <T> processFragment(
        fragment: KmModuleFragment,
        action: (List<KmClass>, List<KmFunction>, List<KmProperty>, List<KmTypeAlias>) -> T
): T {
    val classes = fragment.classes
    val pkg = fragment.pkg
    return when {
        pkg != null -> action(classes, pkg.functions, pkg.properties, pkg.typeAliases)
        else -> action(classes, emptyList(), emptyList(), emptyList())
    }
}

private fun compareMetadata(
        comparisonConfig: ComparisonConfig,
        metadataModuleA: KlibModuleMetadata,
        metadataModuleB: KlibModuleMetadata
): MetadataCompareResult {
    val fragmentA = metadataModuleA.fragments.fold(KmModuleFragment(), ::joinFragments)
    val fragmentB = metadataModuleB.fragments.fold(KmModuleFragment(), ::joinFragments)

    val joinedFragments = processFragment(fragmentA) { classesA, functionsA, propertiesA, typeAliasesA ->
        processFragment(fragmentB) { classesB, functionsB, propertiesB, typeAliasesB ->
            JoinedFragments(
                    buildJoined(classesA, classesB, KmClass::name),
                    buildJoined(functionsA, functionsB, KmFunction::name),
                    buildJoined(propertiesA, propertiesB, KmProperty::name),
                    buildJoined(typeAliasesA, typeAliasesB, KmTypeAlias::name)
            )
        }
    }

    val comparator = KmComparator(comparisonConfig)
    return joinedFragments.run {
        listOf(
                compareElements(comparisonConfig, classes.joined, comparator::compare),
                compareElements(comparisonConfig, functions.joined, comparator::compare),
                compareElements(comparisonConfig, properties.joined, comparator::compare),
                compareElements(comparisonConfig, typeAliases.joined, comparator::compare),
                processMissing(comparisonConfig, this)
        )
    }.wrap()

}

sealed class MetadataCompareResult {
    class Fail(
            val children: Collection<Fail>, val message: String? = null
    ) : MetadataCompareResult() {
        constructor(message: String, child: Fail? = null)
                : this(listOfNotNull(child), message)
    }

    object Ok : MetadataCompareResult()
}

// A neat way to have short names internally without polluting client's namespace.
internal typealias Fail = MetadataCompareResult.Fail
internal typealias Ok = MetadataCompareResult.Ok

fun expandFail(fail: Fail, output: (String) -> Unit, padding: String = "") {
    fail.message?.let { output("$padding$it") }
    fail.children.forEach {
        expandFail(it, output, "$padding ")
    }
}

/**
 * Configure what should be tested and what shouldn't.
 *
 * TODO: Add a way to conditionally disable property comparison.
 *  E.g. "If class name is Companion do not compare flags".
 */
interface ComparisonConfig {
    /**
     * Should the declaration be compared at all.
     */
    fun shouldCheckDeclaration(element: String): Boolean

    /**
     * Should we check property of declaration.
     */
    fun <T, R> shouldCheck(property: T.() -> R): Boolean
}

/**
 * Configuration for comparing `metadata` and `sourcecode` cinterop modes.
 */
class CInteropComparisonConfig : ComparisonConfig {
    override fun <T, R> shouldCheck(property: T.() -> R): Boolean = when (property) {
        // Kotlin compiler may incorrectly omit abbreviatedType in some cases.
        KmType::abbreviatedType -> false
        else -> true
    }

    override fun shouldCheckDeclaration(element: String): Boolean = when {
        // kniBridge is generated only in sourcecode mode.
        element.startsWith("kniBridge") -> false
        else -> true
    }
}

/**
 * Structurally compares metadata of given libraries.
 */
fun compareKlibMetadata(
        comparisonConfig: ComparisonConfig,
        pathToFirstLibrary: String,
        pathToSecondLibrary: String
): MetadataCompareResult {
    val resolveStrategy = CompilerSingleFileKlibResolveAllowingIrProvidersStrategy(
            knownIrProviders = listOf("kotlin.native.cinterop")
    )
    val klib1 = resolveSingleFileKlib(File(pathToFirstLibrary), strategy = resolveStrategy)
    val klib2 = resolveSingleFileKlib(File(pathToSecondLibrary), strategy = resolveStrategy)
    val metadata1 = KlibModuleMetadata.read(TrivialLibraryProvider(klib1), SortedMergeStrategy())
    val metadata2 = KlibModuleMetadata.read(TrivialLibraryProvider(klib2), SortedMergeStrategy())
    return compareMetadata(comparisonConfig, metadata1, metadata2)
}