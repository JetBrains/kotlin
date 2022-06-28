// p.C
package p

interface A {
    fun a() = "a"
}

interface B: A {
    fun b() = "b"
}

interface C : B {
    fun c() = "c"
}

// TODO: could be lazy
// see KT-22819
// LAZINESS:NoLaziness