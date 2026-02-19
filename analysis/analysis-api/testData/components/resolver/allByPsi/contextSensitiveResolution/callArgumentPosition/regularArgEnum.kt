// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType`

enum class MyEnum {
    EnumValue1, EnumValue2;
}
fun receiveEnum(arg: MyEnum) {}

fun test0() {
    val arg1 = MyEnum.EnumValue1
    val arg2 = EnumValue2

    receiveEnum(EnumValue2)
    receiveEnum(arg1)
    receiveEnum(arg2)
    receiveEnum(arg3)
}
