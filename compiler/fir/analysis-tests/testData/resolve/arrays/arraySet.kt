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
    a[0] = B() // set
}

fun test_2(a: A<C>) {
    a[0] = C() // set
}

fun test_3(a: A<D>) {
    a[0] = D() // set
}