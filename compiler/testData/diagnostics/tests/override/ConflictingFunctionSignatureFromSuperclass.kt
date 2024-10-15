// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
open class Aaa() {
    fun foo() = 1
}

open class Bbb() : Aaa() {
    <!CONFLICTING_OVERLOADS!>fun <T> foo()<!> = 2
}
