// p.B
package p

interface A {
    fun a() = "a"
}

interface B: A {
    fun b() = "b"
}

// TODO: could be lazy
// see KT-22819
// LAZINESS:NoLaziness