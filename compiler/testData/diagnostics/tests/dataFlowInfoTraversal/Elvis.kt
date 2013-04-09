fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x ?: 0)
    if (x != null) bar(<!USELESS_ELVIS!>x<!> ?: x)
    bar(<!TYPE_MISMATCH!>x<!>)
}