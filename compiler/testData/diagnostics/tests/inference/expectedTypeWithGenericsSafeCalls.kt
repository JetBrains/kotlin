// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ExpectedTypeFromCast

class X {
    fun <T> foo(): T = TODO()
}

fun test(x: X?) {
    val y = x?.foo() as Int
}
