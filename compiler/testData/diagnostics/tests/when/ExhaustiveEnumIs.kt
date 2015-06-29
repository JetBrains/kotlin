enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    return when (x) {
        is MyEnum.A -> 1
        is MyEnum.B -> 2
        is MyEnum.C -> 3
    }
}