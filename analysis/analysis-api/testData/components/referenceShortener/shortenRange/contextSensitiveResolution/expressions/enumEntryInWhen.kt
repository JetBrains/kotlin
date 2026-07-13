// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

enum class MyEnum {
    A, B
}

fun test(e: MyEnum) {
    when (e) {
        <expr>MyEnum.A</expr> -> {}
        else -> {}
    }
}
