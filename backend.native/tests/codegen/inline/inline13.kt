open class A<T1>()
class B<T2>() : A<T2>()

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T: A<*>> foo(f: Any?): Boolean {
    return f is T?
}

fun bar(): Boolean {
    return foo<B<Int>>(B<Int>())
}

fun main(args: Array<String>) {
    println(bar().toString())
}
