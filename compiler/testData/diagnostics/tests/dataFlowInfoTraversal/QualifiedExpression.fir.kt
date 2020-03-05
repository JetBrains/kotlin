fun baz(x: Int): Int = x + 1

class A {
    fun bar(x: Int) = baz(x)
}

fun foo() {
    val x: Int? = null

    A().<!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    if (x == null) return
    A().bar(x)
}
