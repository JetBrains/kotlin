class ReadByAnotherPropertyInitializer() {
    val a = 1
    init {
        val <!UNUSED_VARIABLE!>x<!> = <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!>
    }
}
