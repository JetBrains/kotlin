fun useBoolean(b: Boolean) {}

fun main() {
    class A {
        fun foo(x: Int) = bar(x)
        fun bar(y: Int) = this.hashCode() + y > 0

        val w get() = z
        val z get() = this.hashCode() == 0
    }

    val a = A()

    useBoolean(a.foo(1))
    useBoolean(a.bar(1))
    useBoolean(a.w)
    useBoolean(a.z)

    class B {
        fun foo(x: Int) = inner.w
        fun bar(y: Int) = this.hashCode() + y > 0

        val inner = Inner()

        inner class Inner {
            val w get() = z
            val z get() = bar(1)
        }
    }

    val b = B()

    useBoolean(b.foo(1))
    useBoolean(b.bar(1))
    useBoolean(b.inner.w)
    useBoolean(b.inner.z)
}
