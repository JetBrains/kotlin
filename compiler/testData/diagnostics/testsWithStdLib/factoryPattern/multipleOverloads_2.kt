// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION -EXPERIMENTAL_API_USAGE -EXPERIMENTAL_UNSIGNED_LITERALS
// ISSUE: KT-11265

// FILE: FactoryPattern.kt

package annotations

annotation class FactoryPattern

// FILE: main.kt

import annotations.FactoryPattern

@FactoryPattern
fun <T, R : Comparable<R>> Iterable<T>.myMaxOf(selector: (T) -> R): R = TODO()
@FactoryPattern
fun <T> Iterable<T>.myMaxOf(selector: (T) -> Double): Double = TODO()
@FactoryPattern
fun <T> Iterable<T>.myMaxOf(selector: (T) -> Float): Float = TODO()

fun Double.pow(v: Int): Double = this

fun test() {
    val value = listOf(1, 2, 3, 4, 5, 6).myMaxOf { -2.0.pow(it) }
    takeDouble(value)
}

fun takeDouble(value: Double) {}