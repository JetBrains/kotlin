// WITH_STDLIB
// MODULE: lib
// FILE: A.kt

inline class A(val x: String) {
    inline fun f(other: A): A = other
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return A("Fail").f(A("OK")).x
}
