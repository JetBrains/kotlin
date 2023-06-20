// ORIGINAL: /compiler/testData/diagnostics/tests/inference/emptyIntersectionTypes/kt48987.fir.kt
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun box(): String {
    return try {
        val range1 = 0..1
        range1 as List<Double>
        range1.joinToString { "" }
    } catch (e: java.lang.ClassCastException) {
        "OK"
    }
}
