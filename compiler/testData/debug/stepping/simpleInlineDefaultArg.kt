// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
inline fun alsoInline() = "OK"

inline fun ifoo(s: String = alsoInline()): String {
    return s
}

fun box(): String {
    return ifoo()
}

// LINENUMBERS
// test.kt:10
// test.kt:5
// test.kt:3
// test.kt:6
// test.kt:10