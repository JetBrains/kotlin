fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x ?: 0)
    if (x != null) bar(x ?: x)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
}