suspend fun foo(action: suspend () -> Unit) {
    val <!UNUSED_VARIABLE!>x<!> = action

    x()
}
