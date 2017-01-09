enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>A<!><!> -> 1
        is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>B<!><!> -> 2
        is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>C<!><!> -> 3
    }
}