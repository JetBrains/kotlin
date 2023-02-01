// ISSUE: KT-54473

interface I
class A : I
class B : I

fun test(a: A, b: B) {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
    a == b as I
}
