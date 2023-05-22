// IGNORE_BACKEND: JS
// WITH_STDLIB

// FILE: 1.kt
inline fun test() {
    val localX = (TODO() as String)::plus
}

// FILE: 2.kt
fun box(): String {
    try {
        test()
    } catch (e: NotImplementedError) {
    }
    return "OK"
}