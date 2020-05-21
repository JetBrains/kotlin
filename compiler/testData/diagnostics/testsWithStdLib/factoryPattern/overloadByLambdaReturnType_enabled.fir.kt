// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION
// ISSUE: KT-11265

// FILE: OverloadResolutionByLambdaReturnType.kt

package kotlin

annotation class OverloadResolutionByLambdaReturnType

// FILE: main.kt

import kotlin.OverloadResolutionByLambdaReturnType

@OverloadResolutionByLambdaReturnType
fun create(f: (Int) -> Int): Int = 1
fun create(f: (Int) -> String): String = ""

fun takeString(s: String) {}
fun takeInt(s: Int) {}

fun test_1() {
    val x = <!AMBIGUITY!>create<!> { "" }
    takeString(x)
}

fun test_2() {
    val x = <!AMBIGUITY!>create<!> { 1 }
    takeInt(x)
}

fun test_3() {
    val x = <!AMBIGUITY!>create<!> { 1.0 }
}

@OverloadResolutionByLambdaReturnType
fun <K> create(x: K, f: (K) -> Int): Int = 1
fun <T> create(x: T, f: (T) -> String): String = ""

fun test_4() {
    val x = <!AMBIGUITY!>create<!>("") { "" }
    takeString(x)
}

fun test_5() {
    val x = <!AMBIGUITY!>create<!>("") { 1 }
    takeInt(x)
}