// ORIGINAL: /compiler/testData/diagnostics/tests/regressions/kt9633.fir.kt
// WITH_STDLIB
// KT-9633: SOE occurred before
interface A<T : A<in T>>

fun box() = "OK"
