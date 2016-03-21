fun foo() {
    var x: Int
    fun bar() {
        x = 42
    }
    <!UNINITIALIZED_VARIABLE!>x<!>.hashCode()
    bar()
}
