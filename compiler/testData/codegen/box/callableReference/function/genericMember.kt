class A<T>(val t: T) {
    fun foo(): T = t
}

fun box() = (A<String>::foo)(A("OK"))
