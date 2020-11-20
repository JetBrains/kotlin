// FILE: test.kt

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

class C {
    fun member(vararg xs: String) {}
}

fun C.extension(vararg xs: String) {}

fun capture1(fn: C.(String) -> Unit): Any = fn
fun capture2(fn: C.(Array<String>) -> Unit): Any = fn

fun box(): String {
    checkNotEqual(capture1(C::member), capture2(C::member))
    checkNotEqual(capture1(C::member), captureMemberFromOtherFile())

    checkNotEqual(capture1(C::extension), capture2(C::extension))
    checkNotEqual(capture1(C::extension), captureExtensionFromOtherFile())
    return "OK"
}

// FILE: fromOtherFile.kt

fun captureMemberFromOtherFile(): Any = capture2(C::member)
fun captureExtensionFromOtherFile(): Any = capture2(C::extension)
