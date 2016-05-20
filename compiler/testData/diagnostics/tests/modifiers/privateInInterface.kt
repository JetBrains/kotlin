interface My {
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> val x: Int
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> val xx: Int
    private val xxx: Int
        get() = 0
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> val y: Int
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> val yy: Int
        get() = 1
    <!PRIVATE_FUNCTION_WITH_NO_BODY!>private<!> fun foo(): Int
    // ok
    private fun bar() = 42
}
