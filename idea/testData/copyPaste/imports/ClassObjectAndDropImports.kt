package a

interface T

class A {
    companion object: T {
    }
}

class B {
    companion object {
        fun f(): Int
    }
}

<selection>fun g(t: T): Int {
    g(A)
    B.f()
    A
    B
    A.Companion
    B.Companion
}</selection>


