fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(if (x == null) 0 else x)

    if (x == null) {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
        return
    } else {
        bar(x)
    }
    bar(x)

    val y: Int? = null
    if (y is Int) {
        bar(y)
    } else {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
        return
    }
    bar(y)

    val z: Int? = null
    if (z != null) bar(z)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(z)
    bar(z!!)
    if (z != null) bar(z!!)
}
