// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM_IR
// IGNORE_HEADER_MODE: JVM_IR
// WITH_STDLIB

class View {
    val coefficient = 42
}

context(View) val Int.dp get() = coefficient * this

fun box(): String {
    with(View()) {
        if (listOf(1, 2, 10).map { it.dp } == listOf(42, 84, 420)) {
            return "OK"
        }
        return "fail"
    }
}
