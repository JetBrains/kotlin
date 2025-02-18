// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// IGNORE K1
// It's expected that in the fir.k1.txt dump, there are no context parameters because K1 doesn't handle them properly.
// FirLoadK1CompiledJvmKotlinTestGenerated should actually just fail but instead it ignores frontend errors and compiles
// the code as if there are no context parameters.
package test

interface A
interface B

annotation class Ann

class C {
    context(@Ann c: B) fun f() {}
    context(@Ann c: B) val p: Int get() = 42
}

context(@Ann c: A) fun f() {}
context(@Ann c: B) val p: Int get() = 42

context(@Ann _: A) fun fUnnamed() {}
context(@Ann _: B) val pUnnamed: Int get() = 42
