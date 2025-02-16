// FILE: base.kt
inline fun base(): String {
    return "OK"
}

inline fun bok() {
    baz()
}

// FILE: dep.kt
inline fun foo(): String {
    return base()
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