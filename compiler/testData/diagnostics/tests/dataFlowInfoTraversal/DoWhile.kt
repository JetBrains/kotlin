fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    do {
        bar(<!TYPE_MISMATCH!>x<!>)
    } while (x == null)
    bar(x)
    
    val y: Int? = null
    do {
        bar(<!TYPE_MISMATCH!>y<!>)
    } while (y != null)
    bar(<!TYPE_MISMATCH!>y<!>)
}
