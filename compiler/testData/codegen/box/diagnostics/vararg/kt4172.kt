fun box(): String {
    main(array())
    return "OK"
}

fun main(args: Array<String>) {
    D.foo(array())
}

object D {
    fun foo(array: Array<out String>) = array
}

inline fun <reified T> array(vararg t : T) : Array<T> = t as Array<T>