//FILE: test.kt
fun box() {
    val k = if (getA()
        && getB()
        && getC()
        && getD()) {
        true
    } else {
        false
    }
}

fun getA() = true

fun getB() = true

fun getC() = false

fun getD() = true

// IGNORE_BACKEND: JVM_IR
// IR backend is missing a line number 3 in box() after stepping on line 9 for getting the result of false

// LINENUMBERS
// TestKt.box():3
// TestKt.getA():13
// TestKt.box():3
// TestKt.box():4
// TestKt.getB():15
// TestKt.box():4
// TestKt.box():5
// TestKt.getC():17
// TestKt.box():5
// TestKt.box():9
// TestKt.box():3
// TestKt.box():11
