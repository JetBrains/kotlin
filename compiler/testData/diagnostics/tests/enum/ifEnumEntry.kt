enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = if (me is <!IS_ENUM_ENTRY!>MyEnum.<!ENUM_ENTRY_AS_TYPE!>FIRST<!><!>) true else false