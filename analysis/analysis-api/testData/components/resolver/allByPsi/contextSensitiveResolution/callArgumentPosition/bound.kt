// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

fun <T: MyEnum> boundHolder(arg: T) = arg

fun testFunBound() {
    boundHolder(EnumValue1)
}

fun testFunBound2() {
    boundHolder<MyEnum>(EnumValue1)
}

inline fun <T: MyEnum> testInlineFunBound(value: T) {
    value == EnumValue1
}

fun <T: MyEnum> testFunBound(value: T) {
    value == EnumValue1
}
