// LANGUAGE: +ContextParameters
// IGNORE_FIR_METADATA_LOADING_K1
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
