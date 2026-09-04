// Kotlin/JS does not throw on integer division by zero
// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.first
var log = ""

fun box(): String {
    val z = 0
    try {
        // the declaration is kept although the sequence is fused at its use, so the element is evaluated here like in the stdlib
        val s = sequenceOf(1, 1 / z)
        log += "declared"
        s.first()
    } catch (e: ArithmeticException) {
        return if (log == "") "OK" else "Fail: exception thrown after $log"
    }
    return "Fail: no exception"
}
