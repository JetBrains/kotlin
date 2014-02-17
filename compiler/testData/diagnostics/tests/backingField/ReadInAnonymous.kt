class ReadByAnotherPropertyInitializer() {
    val a = 1
    {
        val <!UNUSED_VARIABLE!>x<!> = $a
    }
}
