// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: context receivers aren't yet supported

context(String, Int)
fun overloaded(value: Any?) = "OK"

context(String)
fun overloaded(value: Any?) = "fail"

fun box() = with("42") {
    with(42) {
        overloaded(null)
    }
}