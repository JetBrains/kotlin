package a

trait T

class A {
    default object: T {
    }
}

class B {
    default object {
        fun f(): Int
    }
}

<selection>fun g(t: T): Int {
    g(A)
    B.f()
    A
    B
    A.Default
    B.Default
}</selection>


