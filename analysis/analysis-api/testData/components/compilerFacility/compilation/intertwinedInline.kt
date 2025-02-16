// FILE: dep.kt
inline fun foo(): String {
    return "OK"
}

inline fun bar() {
    baz()
}

// FILE: main.kt
inline fun baz() {
    foo()
}

fun main() {
    foo()
}