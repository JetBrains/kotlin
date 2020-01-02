// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

inline fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.box():10
// TestKt.box():5
// TestKt.box():6
// TestKt.box():11
// TestKt.box():7
