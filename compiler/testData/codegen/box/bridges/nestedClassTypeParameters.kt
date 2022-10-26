class Outer {
    class Inner {
        fun foo() = "K"
    }
}

interface I<T> {
    fun foo(t: T, inner: Outer.Inner): String
}

open class Child : I<String> {
    override fun foo(t: String, inner: Outer.Inner): String {
        return t + inner.foo()
    }
}

fun box(): String {
    val child: I<String> = Child()
    return child.foo("O", Outer.Inner())
}