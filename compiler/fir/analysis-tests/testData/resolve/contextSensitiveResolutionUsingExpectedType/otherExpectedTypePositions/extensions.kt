// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

class MyClass {
    companion object {
        val MyClass.Companion.bar: MyClass get() = MyClass()
    }
}

fun foo(a: MyClass) {}

fun main() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>MyClass.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!><!>)
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
}

class MyClass2 {
    companion object
}

val MyClass2.Companion.bar: MyClass2 get() = MyClass2()

fun foo2(a: MyClass2) {}

fun main2() {
    foo2(MyClass2.bar)
    foo2(bar)
}