// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION
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
