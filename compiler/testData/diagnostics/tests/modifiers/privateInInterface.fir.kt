interface My {
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> val x: Int
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> val xx: Int
    private val xxx: Int
        get() = 0
    final val y: Int
    final val yy: Int
        get() = 1
    private fun foo(): Int
    // ok
    private fun bar() = 42
}
