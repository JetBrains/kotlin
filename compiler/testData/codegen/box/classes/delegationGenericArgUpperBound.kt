interface A<T: Number> {
    fun foo(t: T): String
}

class Derived(a: A<Int>) : A<Int> by a

fun box(): String {
    val o = object : A<Int> {
        override fun foo(t: Int) = if (t == 42) "OK" else "Fail $t"
    }
    return Derived(o).foo(42)
}
