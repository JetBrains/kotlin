// RUN_PIPELINE_TILL: FRONTEND
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
    foo(WrongEnum.<!UNRESOLVED_REFERENCE!>X<!>)
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
        foo(<!ARGUMENT_TYPE_MISMATCH, DSL_SCOPE_VIOLATION!>X<!>)
    }
}
