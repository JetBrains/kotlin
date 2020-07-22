enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        MyEnum.A -> 1
        is <!OTHER_ERROR!>MyEnum.B<!> -> 2
        is <!OTHER_ERROR!>MyEnum.C<!> -> 3
    }
}