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
// test.kt:10 box
// test.kt:5 box
// test.kt:3 box
// test.kt:6 box
// test.kt:10 box