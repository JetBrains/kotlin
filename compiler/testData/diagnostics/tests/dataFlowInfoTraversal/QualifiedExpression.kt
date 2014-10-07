fun baz(x: Int): Int = x + 1

class A {
    fun bar(x: Int) = baz(x)
}

fun foo() {
    val x: Int? = null

    A().bar(<!TYPE_MISMATCH!>x<!>)
    if (x == null) return
    A().bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
