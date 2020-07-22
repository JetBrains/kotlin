enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = if (me is <!OTHER_ERROR!>MyEnum.FIRST<!>) true else false