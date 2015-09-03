class A
class B
class C
class D
class E

fun foo(a: A){}
fun foo(b: B, c: C){}
fun foo(d: D, e: E, a: A){}

fun f(pa: A, pb: B, pc: C, pd: D, pe: E) {
    foo(pa, <caret>)
}

// EXIST: pc, pe
// ABSENT: pa, pb, pd
