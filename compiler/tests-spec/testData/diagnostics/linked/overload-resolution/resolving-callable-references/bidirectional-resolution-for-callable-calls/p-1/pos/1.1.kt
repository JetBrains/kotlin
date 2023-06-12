// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-413
 * MAIN LINK: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 1
 * overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 2
 * overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: SAM-type against similar functional type
 */

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-39220
 */
package testPackCase1

import kotlin.reflect.KFunction2

interface Foo {
    fun resolve(var1: Int): String
    fun resolve(var1: String): String
}

fun <T> bar(f: KFunction2<T, String, String>) {}

fun <T : Foo> main() {
    bar<T>(Foo::resolve) // OK in OI, Ambiguity in NI
    bar<Foo>(Foo::resolve) // OK
    bar(Foo::resolve) // OK
}
