// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(a: MyEnum) {}

fun <X> id(x: X): X = TODO()

fun main() {

    val L = MyEnum.X

    foo(id(X))
    foo(id(L))
    foo(id(<!UNRESOLVED_REFERENCE!>UNRESOLVED<!>))
}
