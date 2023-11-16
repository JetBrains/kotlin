// !LANGUAGE: +IntrinsicConstEvaluation
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
fun <T> T.id() = this

const val trimIndent = "123".<!EVALUATED("123")!>trimIndent()<!>
const val complexTrimIndent =
    """
            ABC
            123
            456
        """.<!EVALUATED("ABC\n123\n456")!>trimIndent()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (trimIndent.id() != "123") return "Fail 1"
    if (complexTrimIndent.id() != "ABC\n123\n456") return "Fail 2"
    return "OK"
}
