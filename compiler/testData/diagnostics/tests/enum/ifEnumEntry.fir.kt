enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = if (me is MyEnum.FIRST) true else false