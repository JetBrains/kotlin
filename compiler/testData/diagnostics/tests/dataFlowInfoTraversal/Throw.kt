fun bar(x: Int): RuntimeException = RuntimeException(x.toString())

fun foo() {
    val x: Int? = null

    if (x == null) throw bar(<!TYPE_MISMATCH!>x<!>)
    throw bar(x)
    throw <!UNREACHABLE_CODE!>bar(x)<!>
}
