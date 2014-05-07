class A<T>(val t: T) {
    fun foo(): T = t
}

fun box() = A("OK").(A<String>::foo)()
