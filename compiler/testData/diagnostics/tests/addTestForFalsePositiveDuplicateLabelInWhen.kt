// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-26045

// MODULE: lib
class A {
    fun foo() {}
    fun bar() {}
}

// MODULE: app(lib)
fun test(a: A) {
    a.foo()
    a.bar()
}
