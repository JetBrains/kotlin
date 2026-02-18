// FILE: lib.kt

inline fun foo(x: () -> Unit): String {
    x()
    return "OK"
}

// FILE: main.kt
fun String.id(s: String = this, vararg xs: Int): String = s

fun box(): String = foo("Fail"::id)
