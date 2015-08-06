<!WRONG_MODIFIER_TARGET!>sealed<!> enum class SealedEnum {
    FIRST, 
    SECOND;

    class Derived: SealedEnum()
}
