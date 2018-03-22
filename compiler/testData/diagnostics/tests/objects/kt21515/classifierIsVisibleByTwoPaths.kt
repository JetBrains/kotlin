open class A {
    class FromA {
        fun foo() = 42
    }
}

class B : A() {
    companion object : A() { }

    // we're seeing FromA here by two paths: one is deprecated (via companion object), and another one is not,
    // so we shouldn't see deprecation warning
    val a: FromA? = null

    val b = FromA::foo

    val c = FromA()
}