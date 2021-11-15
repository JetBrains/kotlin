// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

import kotlin.reflect.KClass

fun box(): String {
    val ann1 = kotlin.SinceKotlin("1.6.0")
    val expectedToString = "@kotlin.SinceKotlin(version=1.6.0)"
    val actualToString = ann1.toString()
    if (actualToString != expectedToString) return "Expected ann1.toString() equals to $expectedToString, but it's $actualToString"
    return "OK"
}
