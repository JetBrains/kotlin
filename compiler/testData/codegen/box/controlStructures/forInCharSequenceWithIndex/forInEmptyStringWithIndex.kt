// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    for ((index, x) in "".withIndex()) {
        return "Loop over empty String should not be executed"
    }
    return "OK"
}