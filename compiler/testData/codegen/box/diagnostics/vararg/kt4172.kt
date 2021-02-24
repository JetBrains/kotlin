fun box(): String {
    main(array())
    return "OK"
}

fun main(args: Array<String>) {
    args.size
    D.foo(array())
}

object D {
    fun foo(array: Array<out String>) = array
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> array(vararg t : T) : Array<T> = t as Array<T>