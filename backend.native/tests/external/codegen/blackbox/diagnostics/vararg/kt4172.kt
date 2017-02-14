fun box(): String {
    foo(array())
    return "OK"
}

fun foo(args: Array<String>) {
    D.foo(array())
}

object D {
    fun foo(array: Array<out String>) = array
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> array(vararg t : T) : Array<T> = t as Array<T>