// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum1 {
    X, Y
}

enum class MyEnum2 {
    X, Y
}

enum class MyEnum3 {
    X, Y
}

fun foo(x: MyEnum1) {}
fun foo(x: MyEnum2) {}

fun bar(x: MyEnum3) {}

fun <X> id(x: X): X = TODO()

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>X<!>)
    foo(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>X<!>))
    foo(MyEnum1.X)
    foo(id(MyEnum1.X))

    bar(X)
    bar(id(X))
}

fun main2() {
    fun bar(x: MyEnum1) {}
    fun bar(x: MyEnum2) {}

    bar(<!UNRESOLVED_REFERENCE!>X<!>)
    bar(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>X<!>))

    bar(MyEnum3.X)
}
