class ReadNonexistent() {
    val a: Int
        get() = 1

    init {
        val <!UNUSED_VARIABLE!>x<!> = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
    }
}
