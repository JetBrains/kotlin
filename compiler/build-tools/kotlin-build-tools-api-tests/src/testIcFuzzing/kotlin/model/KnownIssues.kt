/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.model

/**
 * The tracked IC bugs the fuzzer would otherwise keep finding instead of new ones.
 * The edits reproducing them are generated only when they are included by the `kotlin.ic.fuzz.includeKnownIssues` system property:
 * `true` includes all of them, otherwise it is a comma-separated list of the names of the properties below.
 */
object KnownIssues {
    private val includedNames = System.getProperty("kotlin.ic.fuzz.includeKnownIssues").orEmpty().split(',').map(String::trim).toSet()

    private fun included(name: String): Boolean = "true" in includedNames || name in includedNames

    /**
     * KT-89522: a changed default value of an inline function is not propagated to its call sites. The value also changes
     * with the class of its type, e.g. when the class becomes abstract. When excluded, inline functions have only defaults of builtin types.
     */
    val inlineDefaultValueChange: Boolean get() = included("inlineDefaultValueChange")

    /** A change visible only in the `$$forInline` template of a suspend inline function is not propagated to its call sites. */
    val suspendInlineForInlineTemplate: Boolean get() = included("suspendInlineForInlineTemplate")

    /**
     * KT-89613, KT-89649: a change of a class owned by an inline function, here an anonymous object capturing a crossinline lambda,
     * is not propagated to its call sites, which have its copy. This includes the SMAP of the class, which changes with the number
     * of lines of its file. When excluded, crossinline lambdas are called directly.
     */
    val inlineFunOwnedClass: Boolean get() = included("inlineFunOwnedClass")

    /**
     * An inline function compiled together with its call site is inlined unoptimized, and optimized when it is read from a class file,
     * so recompiling only some of the files gives a different bytecode, as in KT-19175. This shows in the `$$forInline` templates
     * of suspend inline functions, and in the code inlined through another inline function or an inline property.
     * When excluded, inline functions are not suspend, have no `noinline` parameters or local variables, do not call inline functions
     * or read inline properties, and reified ones have no `run` body.
     * The reproducers are in `SeparateCompilationReproducibilityTest` of the `testIcFuzzingReproducers` suite.
     */
    val inlineOfSeparateCompilation: Boolean get() = included("inlineOfSeparateCompilation")

    /**
     * A class compiled together with a supertype from another file and one compiled against its class file may differ in the order
     * of their constant pools and members, e.g. a class implementing an interface with default members. When excluded, the class files
     * with the same `javap` dumps up to the order of the members are equal.
     */
    val classFileOrder: Boolean get() = included("classFileOrder")
}
