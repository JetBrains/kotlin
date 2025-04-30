// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-76776

@MustUseReturnValue
class A {
    companion object {
        fun foo(): Int = 123
    }

    class Nested {
        fun bar(): Int = 123
    }
}

fun test() {
    A.foo()                 //unused
    A.Nested().bar()        //unused
}