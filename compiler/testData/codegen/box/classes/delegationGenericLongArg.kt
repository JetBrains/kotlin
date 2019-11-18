// IGNORE_BACKEND_FIR: JVM_IR
interface A<T, U> {
    fun foo(t: T, u: U): String
}

class Derived(a: A<Long, Int>) : A<Long, Int> by a

fun box(): String {
    val o = object : A<Long, Int> {
        override fun foo(t: Long, u: Int) = if (t == u.toLong()) "OK" else "Fail $t $u"
    }
    return Derived(o).foo(42, 42)
}
