package a

object B {
    fun f()
}

trait T

object A: T {
}

<selection>fun g(t: T) {
    g(A)
    B.f()
}</selection>