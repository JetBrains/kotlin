// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields


interface Base {
    val a : Any
        get() = "not OK"
}

class Derived : Base {
    final override val a: Any
        field: String = "OK"

    fun usage(): String {
        return acceptString(a)
    }
}

fun acceptString(a: String): String {
    return a
}

fun box(): String {
    return Derived().usage()
}
