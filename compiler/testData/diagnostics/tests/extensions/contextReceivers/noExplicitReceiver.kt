// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// FIR_IDENTICAL

class A
class B
class C

context(A) fun B.f() {}
context(A) fun B.g() {
    f()
}
context(A) fun C.h() {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>()
}

fun A.q(b: B) {
    with(b) {
        f()
    }
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>()
}