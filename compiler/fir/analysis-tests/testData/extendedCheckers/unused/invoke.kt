fun foo(): Int {
    val <!UNUSED_VARIABLE!>x<!> = fun() = 4
    val <!UNUSED_VARIABLE!>y<!> = fun() = 2
    return 10 * x() + y()
}
