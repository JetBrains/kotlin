// ORIGINAL: /compiler/testData/diagnostics/tests/typealias/returnTypeNothingShouldBeSpecifiedExplicitly.fir.kt
// WITH_STDLIB
typealias N = Nothing

fun testFun(): N = null!!
val testVal: N = null!!
val testValWithGetter: N get() = null!!


fun box() = "OK"
