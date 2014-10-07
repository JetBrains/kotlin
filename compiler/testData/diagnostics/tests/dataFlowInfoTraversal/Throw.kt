fun bar(x: Int): RuntimeException = RuntimeException(x.toString())

fun foo() {
    val x: Int? = null

    if (x == null) throw bar(<!TYPE_MISMATCH!>x<!>)
    throw bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
    <!UNREACHABLE_CODE!>throw bar(<!DEBUG_INFO_SMARTCAST!>x<!>)<!>
}