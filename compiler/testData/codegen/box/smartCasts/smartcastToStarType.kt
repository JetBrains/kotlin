// ISSUE: KT-62863
// FIR_IDENTICAL
// DUMP_IR

interface Base<out T> {
    fun foo(): T
}

class Derived<T> : Base<T> {
    override fun foo(): T = "error" as T
}

fun <T> Derived<T>.bar(): Base<T> = object : Base<T> {
    override fun foo(): T = "OK" as T
}

fun <T> test(x: Base<T>): T {
    if (x is Derived<*>) {
        val y: Base<Any?> = x.bar()
        return y.foo() as T
    }

    return x.foo()
}

fun box(): String {
    val x = Derived<String>()
    return test(x)
}
