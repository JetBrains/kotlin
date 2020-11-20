enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        MyEnum.A -> 1
        is <!UNRESOLVED_REFERENCE!>MyEnum.B<!> -> 2
        is <!UNRESOLVED_REFERENCE!>MyEnum.C<!> -> 3
    }
}