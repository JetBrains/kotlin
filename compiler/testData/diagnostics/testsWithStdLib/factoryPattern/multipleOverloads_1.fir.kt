// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION -EXPERIMENTAL_API_USAGE -EXPERIMENTAL_UNSIGNED_LITERALS
// ISSUE: KT-11265

// FILE: OverloadResolutionByLambdaReturnType.kt

package kotlin

annotation class OverloadResolutionByLambdaReturnType

// FILE: main.kt

import kotlin.OverloadResolutionByLambdaReturnType

@OverloadResolutionByLambdaReturnType
fun <R> UByteArray.fooMap(t: (UByte) -> Iterable<R>): List<R> {
    TODO("ub.fm")
}

@OverloadResolutionByLambdaReturnType
fun <T, R> Iterable<T>.fooMap(t: (T) -> Iterable<R>): List<R> {
    TODO("i.fm(i)")
}

@JvmName("fooMapSeq")
fun <T, R> Iterable<T>.fooMap(t: (T) -> Sequence<R>): List<R> {
    TODO("i.fm(s)")
}

fun test() {
    val list = ubyteArrayOf(0u).<!AMBIGUITY!>fooMap<!> { <!INAPPLICABLE_CANDIDATE!>listOf<!>(<!UNRESOLVED_REFERENCE!>it<!>) }
    takeUByteList(list)
}

fun takeUByteList(list: List<UByte>) {}