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

    fun more(): String
}
