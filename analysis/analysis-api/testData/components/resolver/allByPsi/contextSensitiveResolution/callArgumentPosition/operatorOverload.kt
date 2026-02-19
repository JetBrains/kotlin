// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass() {
    }
}

class OperatorHolder() {
    operator fun plus(enumArg: MyEnum) {}
    operator fun minus(arg: MyClass) {}
}

fun testOperator(i: OperatorHolder) {
    i + EnumValue1
    i + InheritorObject
    i - InheritorObject
}
