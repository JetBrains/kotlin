// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

open class MyClass {
    object InheritorObject: MyClass()

    companion object {
        fun func(): MyClass = TODO()
    }
}

fun defaultArgHolder1(
    enumArg: MyEnum = EnumValue1,
    sealedArg: MyClass = InheritorObject,
    sealedArg2: MyClass = func()
) {}
