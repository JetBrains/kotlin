// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

context(String, Int)
fun overloaded(value: Any?) = "OK"

context(String)
fun overloaded(value: Any?) = "fail"

fun box() = with("42") {
    with(42) {
        overloaded(null)
    }
}