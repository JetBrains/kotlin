//
actual open class A {
    actual open fun c(a: Int, b: String) {}
}

open class B : A() {
    override fun c(a: Int, b: String) {}
}

open class D : B() {
    override fun c(a: Int, b: String) {}
}

fun test(a: Int, b: String) {
    with(A()) {
        c(a, b)
    }
}