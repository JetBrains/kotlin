// FILE: lib.kt
inline fun String.app(f: (String) -> String) = f(this)

// FILE: main.kt
fun fff(s: String, n: Int = 42) = s

fun box() = "OK".app(::fff)
