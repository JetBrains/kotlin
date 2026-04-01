// IDE_MODE
// LANGUAGE: -ContextSensitiveResolutionUsingExpectedType
package test

enum class MyEnum {
    A, B
}

fun expectsMyEnum(e: MyEnum) {}

fun test() {
    expectsMyEnum(<expr>MyEnum.A</expr>)
}
