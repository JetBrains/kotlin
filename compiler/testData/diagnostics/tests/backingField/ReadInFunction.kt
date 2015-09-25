class ReadByAnotherPropertyInitializer() {
    val a = 1
    fun ff() = <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!>
}
