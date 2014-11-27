open class A {
    open fun foo(n: Int,
                 s: String): String = ""
}

class B: A() {
    override fun foo(n: Int,
                     s: String): String = ""
}