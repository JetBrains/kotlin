//FILE: test.kt
fun box() {
    val a = 1
    val b = 2
    try {
        throwIfLess(a, b)
    } catch (e: java.lang.Exception) {
        throwIfLess(a, b)
    }
    throwIfLess(b,a)
}

fun throwIfLess(a: Int, b: Int) {
    if (a<b)
        throw java.lang.IllegalStateException()
}
// IGNORE_BACKEND: JVM_IR
// after throwing exception in try block, IR backend is returning a line number at line 6 instead of line 7 where catch statement is in.
// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.box():5
// TestKt.box():6
// TestKt.throwIfLess(int, int):14
// TestKt.throwIfLess(int, int):15
// TestKt.box():7
// TestKt.box():8
// TestKt.throwIfLess(int, int):14
// TestKt.throwIfLess(int, int):15
