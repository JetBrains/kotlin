// ISSUE: KT-37516

class A<T> {
    operator fun get(index: Int): T = null!!
    operator fun set(index: Int, value: T) {}
}

class B {
    operator fun plusAssign(other: B) {}
}

class C {
    operator fun plus(other: C): C = this
}

class D {
    operator fun plusAssign(other: D) {}
    operator fun plus(other: D): D = this
}

fun test_1(a: A<B>) {
//    foo().bar()[0] += x
    a[0] += B() // get, plusAssign
}

fun test_2(a: A<C>) {
    a[0] += C() // get, set, plus
}

fun test_3(a: A<D>) {
    <!AMBIGUITY!>a[0] += D()<!> // ambiguity
}

fun test_4(b: B) {
    <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>b[0]<!> += B()<!> // unresolved
}