sealed enum class SealedEnum {
    FIRST, 
    SECOND;

    class Derived: SealedEnum()
}
