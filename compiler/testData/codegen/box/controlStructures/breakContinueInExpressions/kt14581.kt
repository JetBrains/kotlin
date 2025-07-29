// FILE: lib.kt
inline fun String.bar(other: String) = this

// FILE: main.kt
fun foo(x: String): String {
    var y: String
    do {
        y = x
    } while (y != x.bar(x))
    return y
}

fun box(): String = foo("OK")