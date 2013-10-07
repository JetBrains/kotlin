trait A {
    fun f(i: Int) {}
}

trait B {
    fun f(i: Int) {}
}

trait C : A, B {

}

fun usage(c: C) {
    c.f()
}