fun bar(): <!UNSUPPORTED!>dynamic<!> = TODO()

fun foo() {
    val x = bar()
    if (x is String) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("dynamic")!>x<!>
    }
}
