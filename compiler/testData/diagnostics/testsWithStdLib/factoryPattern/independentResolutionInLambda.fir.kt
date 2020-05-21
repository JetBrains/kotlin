// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION
// ISSUE: KT-11265

// FILE: OverloadResolutionByLambdaReturnType.kt

package kotlin

annotation class OverloadResolutionByLambdaReturnType

// FILE: main.kt

import kotlin.OverloadResolutionByLambdaReturnType

@kotlin.jvm.JvmName("myFlatMapIterable")
@OverloadResolutionByLambdaReturnType
fun <T, R> Sequence<T>.myFlatMap(transform: (T) -> Iterable<R>): Sequence<R> {
    TODO()
}

fun <T, R> Sequence<T>.myFlatMap(transform: (T) -> Sequence<R>): Sequence<R> {
    TODO()
}

interface A {
    val supertypes: Collection<B>
}

interface B {
    val descriptors: Sequence<C>?
}

interface C

fun <K : Any> elvis(x: K?, y: K): K = y

fun test(a: A) {
    a.supertypes.asSequence().<!AMBIGUITY!>myFlatMap<!> {
        <!INAPPLICABLE_CANDIDATE!>elvis<!>(<!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>descriptors<!>, sequenceOf())
    }
}