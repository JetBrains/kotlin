// ISSUE: KT-72585

interface A<out T : Any> {
    fun foo(): String
}

interface B<out S : Any, out T : Any> : A<T> {
    val x: S
}

class C<out S : Any, out T : Any>(override val x: S) : B<S, T> {
    override fun foo(): String {
        return "OK"
    }
}

class D(val c: C<*, *>) : B<Any, Any> by c

fun box(): String {
    val c = C<Int, String>(1)
    val d = D(c)
    return d.foo()
}
