// The non-reified type parameters are substituted with the type arguments of the call site.

inline fun <T> id(x: T): T {
    val y: T = x
    return y
}

fun test(s: String): String = id(s)
