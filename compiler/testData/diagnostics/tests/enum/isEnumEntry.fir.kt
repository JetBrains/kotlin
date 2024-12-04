// RUN_PIPELINE_TILL: FRONTEND
enum class MyEnum {
    FIRST,
    SECOND
}

fun foo(me: MyEnum): Boolean = me is <!IS_ENUM_ENTRY!>MyEnum.FIRST<!>
