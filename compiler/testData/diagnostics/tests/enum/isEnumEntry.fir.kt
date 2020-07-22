enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = me is <!OTHER_ERROR!>MyEnum.FIRST<!>