// !LANGUAGE: +NewInference +OverloadResolutionByLambdaReturnType
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
    val x = create { "" }
    takeString(x)
}

fun test_2() {
    val x = create { 1 }
    takeInt(x)
}

fun test_3() {
    val x = create { <!ARGUMENT_TYPE_MISMATCH!>1.0<!> }
}

@OverloadResolutionByLambdaReturnType
fun <K> create(x: K, f: (K) -> Int): Int = 1
fun <T> create(x: T, f: (T) -> String): String = ""

fun test_4() {
    val x = create("") { "" }
    takeString(x)
}

fun test_5() {
    val x = create("") { 1 }
    takeInt(x)
}

interface A
interface B
interface C : A, B

@OverloadResolutionByLambdaReturnType
fun foo(f: () -> A): Int = 1
fun foo(f: () -> B): String = ""

fun test_6(c: C) {
    val x = foo { c }
    takeString(x)
}
