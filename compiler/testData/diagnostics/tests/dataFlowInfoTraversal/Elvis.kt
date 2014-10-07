fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x ?: 0)
    if (x != null) bar(<!USELESS_ELVIS!>x<!> ?: <!DEBUG_INFO_SMARTCAST!>x<!>)
    bar(<!TYPE_MISMATCH!>x<!>)
}