class ReadNonexistent() {
    val a: Int
        get() = 1

    {
        val x = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
    }
}
