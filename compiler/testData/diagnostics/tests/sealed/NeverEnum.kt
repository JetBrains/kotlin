<!SEALED_MODIFIER_IN_ENUM!>sealed<!> enum class SealedEnum {
    FIRST, 
    SECOND;

    class Derived: SealedEnum()
}
