// WITH_STDLIB

class Hello {
    val message: String by lazy { name }
    val len = message.length
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val name = "Alice"<!>
}
