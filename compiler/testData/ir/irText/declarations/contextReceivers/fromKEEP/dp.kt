// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

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