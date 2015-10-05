annotation class My(
    public val x: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val y: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> val z: Int,
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> val w: Int
)

open class Your {
    open val x: Int = 0
}

annotation class His(<!WRONG_MODIFIER_CONTAINING_DECLARATION!>override<!> val x: Int): <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Your()<!>