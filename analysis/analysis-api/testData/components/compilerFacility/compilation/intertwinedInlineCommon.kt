// MODULE: common
// TARGET_PLATFORM: Common

// FILE: base.kt
inline fun base(): String {
    return "OK"
}

inline fun bok() {
    bar()
}

// FILE: dep.kt
inline fun foo(): String {
    return base()
}

inline fun bar() {
    1 + 2
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM

// FILE: main.kt
fun main() {
    foo()
}