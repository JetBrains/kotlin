// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType`

enum class MyEnum {
    EnumValue1, EnumValue2;
}


fun receiveEnum(arg: MyEnum) {}

fun test0() {
    val arg1 = MyEnum.EnumValue1
    val arg2 = <!UNRESOLVED_REFERENCE!>EnumValue2<!>

    receiveEnum(EnumValue2)
    receiveEnum(arg1)
    receiveEnum(arg2)
    receiveEnum(<!UNRESOLVED_REFERENCE!>arg3<!>)
}