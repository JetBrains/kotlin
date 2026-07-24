// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

enum class MyEnum {
    A, B
}

fun test() {
    val e: MyEnum = <expr>MyEnum.A</expr>
}
