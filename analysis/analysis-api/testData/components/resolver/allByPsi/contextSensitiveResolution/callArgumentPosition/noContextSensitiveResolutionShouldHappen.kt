// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

enum class WrongEnum {
    A, B
}

fun foo(a: MyEnum) {}

fun baz() {
    foo(X)
    foo(WrongEnum.X)
}

@DslMarker
annotation class MyDsl

@MyDsl
class A

@MyDsl
class B

val A.X: String get() = ""

fun A.foo() {
    fun B.bar() {
        foo(X)
    }
}
