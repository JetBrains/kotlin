// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION
// ISSUE: KT-11265

// FILE: FactoryPattern.kt

package annotations

annotation class FactoryPattern

// FILE: main.kt

import annotations.FactoryPattern

@kotlin.jvm.JvmName("myFlatMapIterable")
@FactoryPattern
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
    a.supertypes.asSequence().myFlatMap {
        elvis(it.descriptors, sequenceOf())
    }
}