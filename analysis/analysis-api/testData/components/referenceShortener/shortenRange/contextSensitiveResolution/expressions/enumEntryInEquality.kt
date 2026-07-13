// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

enum class MyEnum {
    A, B
}

fun test(e: MyEnum) {
    val result = e == <expr>MyEnum.A</expr>
}
