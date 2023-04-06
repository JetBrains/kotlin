// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57435

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
