//FILE: test.kt
fun cond() = false

fun box() {
    if (cond())
        cond()
    else
         false
}

// LINENUMBERS
// TestKt.box():5
// TestKt.cond():2
// TestKt.box():5
// TestKt.box():8
// TestKt.box():9
