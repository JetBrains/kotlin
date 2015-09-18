class ReadByAnotherPropertyInitializer() {
    val a = 1
    val b = <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!>
}
