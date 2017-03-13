fun foo(x: String): String {
    var y: String
    do {
        y = x
    } while (y != x.bar(x))
    return y
}

inline fun String.bar(other: String) = this

fun box(): String = foo("OK")