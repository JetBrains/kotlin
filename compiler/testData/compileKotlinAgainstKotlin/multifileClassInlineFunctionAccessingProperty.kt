// FILE: A.kt

@file:[JvmName("Test") JvmMultifileClass]

val property = "K"

inline fun K(body: () -> String): String =
        body() + property

// FILE: B.kt

fun main(args: Array<String>) {
    val ok = K { "O" }
    if (ok != "OK") throw java.lang.AssertionError("Expected: OK, actual: $ok")
}
