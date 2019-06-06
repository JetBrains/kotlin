//FILE: test.kt
fun box(): Int {
    if (
        getB() ==
        getA())
        return 0
    return getB()
}

fun getA() = 3

inline fun getB(): Int {
    return 1
}

// IGNORE_BACKEND: JVM_IR
// old backend is missing a line number after return from inline function call, actually IR backend results seems more right.

// LINENUMBERS
// TestKt.box():4
// TestKt.box():13
// TestKt.box():5
// TestKt.getA():10
// TestKt.box():5
// TestKt.box():7
// TestKt.box():13
