// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_HEADER_MODE: JVM_IR
// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM_IR

context(String, Int)
fun overloaded(value: Any?) = "OK"

context(String)
fun overloaded(value: Any?) = "fail"

fun box() = with("42") {
    with(42) {
        overloaded(null)
    }
}
