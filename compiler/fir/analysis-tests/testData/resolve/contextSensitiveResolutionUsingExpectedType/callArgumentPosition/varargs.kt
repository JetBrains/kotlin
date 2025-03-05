// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(vararg a: MyEnum) {}

fun main() {
    foo(X, Y)
}
