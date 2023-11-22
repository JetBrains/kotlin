// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// ISSUE: KT-48708

// KT-61141: throws kotlin.Exception instead of java.lang.Exception
// IGNORE_BACKEND: NATIVE

fun test(b: Boolean) {
    val x = if (b) {
        3
    } else {
        throw Exception()
        0
    }
    takeInt(x)
}

fun takeInt(x: Int) {}
