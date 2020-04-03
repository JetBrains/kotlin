fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    do {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    } while (x == null)
    bar(x)
    
    val y: Int? = null
    do {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
    } while (y != null)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
}
