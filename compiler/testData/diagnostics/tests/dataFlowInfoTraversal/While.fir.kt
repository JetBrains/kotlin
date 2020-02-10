fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    while (x == null) {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    }
    bar(x)
    
    val y: Int? = null
    while (y != null) {
        bar(y)
    }
    <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
    
    val z: Int? = null
    while (z == null) {
        <!INAPPLICABLE_CANDIDATE!>bar<!>(z)
        break
    }
    <!INAPPLICABLE_CANDIDATE!>bar<!>(z)
}
