annotation class My(
    public val x: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val y: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> val z: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> val w: Int
)