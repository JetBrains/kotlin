enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = me is <!UNRESOLVED_REFERENCE!>MyEnum.FIRST<!>