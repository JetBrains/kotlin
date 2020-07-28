enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = if (me is <!UNRESOLVED_REFERENCE!>MyEnum.FIRST<!>) true else false