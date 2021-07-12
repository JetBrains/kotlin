class A
class B
class C

context(A) fun B.f() {}
context(A) fun B.g() {
    f()
}
context(A) fun C.h() {
    <!INAPPLICABLE_CANDIDATE!>f<!>()
}

fun A.q(b: B) {
    with(b) {
        f()
    }
    <!INAPPLICABLE_CANDIDATE!>f<!>()
}