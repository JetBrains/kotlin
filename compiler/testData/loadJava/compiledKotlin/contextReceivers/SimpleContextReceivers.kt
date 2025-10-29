// LANGUAGE: +ContextReceivers
// IGNORE_FIR_DIAGNOSTICS

package test

interface A
interface B

context(A) class C {
    context(B) fun f() {}
}

context(A) fun g() {}
context(B) val h: Int get() = 42
