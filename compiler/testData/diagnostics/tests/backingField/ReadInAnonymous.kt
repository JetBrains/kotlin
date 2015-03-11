class ReadByAnotherPropertyInitializer() {
    val a = 1
    init {
        val <!UNUSED_VARIABLE!>x<!> = $a
    }
}
