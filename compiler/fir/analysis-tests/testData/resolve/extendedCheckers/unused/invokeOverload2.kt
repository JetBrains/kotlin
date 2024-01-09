fun foo() {
    val <!UNUSED_VARIABLE!>x<!> = fun(s: String) {}

    fun nested() {
        val x = fun(i: Int) {}

        x(10)
    }
}
