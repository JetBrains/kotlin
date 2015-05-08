enum class MyEnum(val myArg: Int) {
    FIRST(1),
    SECOND: <!ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR!>MyEnum(myArg = 2)<!>,
    THIRD(3),
    FOURTH(4)
}