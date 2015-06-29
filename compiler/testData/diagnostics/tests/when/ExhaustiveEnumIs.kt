enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        is <!IS_ENUM_ENTRY!>MyEnum.A<!> -> 1
        is <!IS_ENUM_ENTRY!>MyEnum.B<!> -> 2
        is <!IS_ENUM_ENTRY!>MyEnum.C<!> -> 3
    }
}