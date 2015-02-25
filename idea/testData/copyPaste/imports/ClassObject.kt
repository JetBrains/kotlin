package a

trait T

class A {
    class object: T {
    }
}

class B {
    class object {
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


