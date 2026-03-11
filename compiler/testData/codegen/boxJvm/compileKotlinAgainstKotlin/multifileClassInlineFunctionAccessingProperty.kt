// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

@file:[JvmName("Test") JvmMultifileClass]

val property = "K"

inline fun K(body: () -> String): String =
        body() + property

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return K { "O" }
}
