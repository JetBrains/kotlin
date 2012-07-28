fun foo(f : () -> Unit) {
    val <!UNUSED_VARIABLE!>x<!> : Unit = f()
}
