fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x ?: 0)
    if (x != null) bar(x <!USELESS_ELVIS!>?: <!DEBUG_INFO_SMARTCAST!>x<!><!>)
    bar(<!TYPE_MISMATCH!>x<!>)
}