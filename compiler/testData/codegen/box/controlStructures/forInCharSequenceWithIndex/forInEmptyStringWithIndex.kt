// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    for ((index, x) in "".withIndex()) {
        return "Loop over empty String should not be executed"
    }
    return "OK"
}