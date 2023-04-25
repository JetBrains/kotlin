// ISSUE: KT-54473

interface I
class A : I
class B : I

fun test(a: A, b: B) {
    a == b
    a == b as I
}
