// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

inline fun f(block: () -> Unit) {
    block()
}

fun g() {}

// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.box():8
// TestKt.box():4
// TestKt.g():11
// TestKt.box():9
// TestKt.box():5
