fun bar(x: Int) = x + 1

fun foo() {
    val x: Int? = null

    fun baz() = <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    fun quux() = if (x != null) bar(x) else baz()
    fun quuux() = bar(if (x == null) 0 else x)
}
