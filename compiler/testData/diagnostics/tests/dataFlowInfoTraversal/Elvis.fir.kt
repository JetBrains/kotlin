fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x ?: 0)
    if (x != null) bar(x <!USELESS_ELVIS!>?: x<!>)
    bar(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
