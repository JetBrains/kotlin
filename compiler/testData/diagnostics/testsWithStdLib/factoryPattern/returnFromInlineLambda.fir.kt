// !LANGUAGE: +NewInference +OverloadResolutionByLambdaReturnType
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION -EXPERIMENTAL_API_USAGE -EXPERIMENTAL_UNSIGNED_LITERALS
// ISSUE: KT-11265

// FILE: OverloadResolutionByLambdaReturnType.kt

package kotlin

annotation class OverloadResolutionByLambdaReturnType

// FILE: main.kt

import kotlin.OverloadResolutionByLambdaReturnType

inline fun <T, R> Array<out T>.myFlatMap(transform: (T) -> Iterable<R>): List<R> {
    TODO()
}

@OverloadResolutionByLambdaReturnType
@JvmName("seqFlatMap")
inline fun <T, R> Array<out T>.myFlatMap(transform: (T) -> Sequence<R>): List<R> {
    TODO()
}

fun String.toList(): List<String> = null!!

fun test_1(a: Array<String>, b: Boolean) {
    a.<!AMBIGUITY!>myFlatMap<!> { <!UNRESOLVED_REFERENCE!>it<!>.toList().ifEmpty { return } }
    a.<!AMBIGUITY!>myFlatMap<!> {
        if (b) return
        <!UNRESOLVED_REFERENCE!>it<!>.toList()
    }
}

fun <T, R> Array<out T>.noInlineFlatMap(transform: (T) -> Iterable<R>): List<R> {
    TODO()
}

@OverloadResolutionByLambdaReturnType
@JvmName("noInlineSeqFlatMap")
fun <T, R> Array<out T>.noInlineFlatMap(transform: (T) -> Sequence<R>): List<R> {
    TODO()
}

fun test_2(a: Array<String>, b: Boolean) {
    a.<!AMBIGUITY!>noInlineFlatMap<!> { <!UNRESOLVED_REFERENCE!>it<!>.toList().ifEmpty { return } }
    a.<!AMBIGUITY!>noInlineFlatMap<!> {
        if (b) return
        <!UNRESOLVED_REFERENCE!>it<!>.toList()
    }
}
