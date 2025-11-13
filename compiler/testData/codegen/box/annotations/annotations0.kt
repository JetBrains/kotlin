// MODULE: a
// FILE: a.kt

inline fun <R> myLet(block: () -> R): R {
    return block()
}

// MODULE: b(a)
// FILE: b.kt

fun box(): String {
    myLet { return "OK" }
}