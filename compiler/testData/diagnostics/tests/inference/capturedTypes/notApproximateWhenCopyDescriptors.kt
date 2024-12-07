// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Bar

fun Bar.foo() = 42

object MyObject {
    fun foo(bar: Bar) = bar.foo()
}