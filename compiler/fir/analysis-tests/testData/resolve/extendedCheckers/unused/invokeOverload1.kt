fun foo() {
    val x = fun(s: String) {}

    fun nested() {
        val <!UNUSED_VARIABLE!>x<!> = fun(i: Int) {}

        x("hello")
    }
}
