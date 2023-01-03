open class A

interface I

external open class B

external class C : A

external class D : B, I

external interface K : I

external enum class E {
    X
}

external enum class F : I {
    X
}
