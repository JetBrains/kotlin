// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
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
