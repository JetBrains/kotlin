// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: Unexpected IR element found during code generation. Either code generation for it is not implemented, or it should have been lowered: ERROR_CALL 'Unresolved reference: <Unresolved name: Foo>#' type=IrErrorType([Error type: Unresolved type for Foo])
// FIR_IDENTICAL
// MODULE: m1
// FILE: test/Foo.java

package test;

class Foo {
    static Foo create() { return Foo(); }
    void takeFoo(Foo f) {}
}

// MODULE: m2(m1)
// FILE: test.kt

package test

fun test() {
    Foo()
    val a: Foo = Foo.create()
    Foo().takeFoo(a)
}
