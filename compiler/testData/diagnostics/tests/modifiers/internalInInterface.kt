interface My {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> val x: Int
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> val xxx: Int
        get() = 0
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> fun foo(): Int
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> fun bar() = 42
}
