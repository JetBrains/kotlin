class Hello {
    val message: String by lazy { name }
    <!UNREACHABLE_CODE!>val len = message.length<!>
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val name = "Alice"<!>
}