// IGNORE_BACKEND: NATIVE
// FILE: A.kt

@file:[JvmName("Test") JvmMultifileClass]

val property = "K"

inline fun K(body: () -> String): String =
        body() + property

// FILE: B.kt

fun box(): String {
    return K { "O" }
}
