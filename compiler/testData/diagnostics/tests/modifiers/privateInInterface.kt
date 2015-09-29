interface My {
    <!PRIVATE_PROPERTY_IN_TRAIT!>private<!> val x: Int
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!ABSTRACT_MODIFIER_IN_TRAIT, INCOMPATIBLE_MODIFIERS!>abstract<!> val xx: Int
    private val xxx: Int
        get() = 0
    <!FINAL_PROPERTY_IN_TRAIT, DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> val y: Int
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> val yy: Int
        get() = 1
    <!PRIVATE_FUNCTION_WITH_NO_BODY!>private<!> fun foo(): Int
    // ok
    private fun bar() = 42
}
