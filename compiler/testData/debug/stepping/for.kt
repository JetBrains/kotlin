//FILE: test.kt
fun box() {
    for (i in 1..3) {
        foo(i)
    }
}

inline fun foo(n: Int) {}

// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.box():8
// TestKt.box():3
// TestKt.box():4
// TestKt.box():8
// TestKt.box():3
// TestKt.box():4
// TestKt.box():8
// TestKt.box():3
// TestKt.box():6