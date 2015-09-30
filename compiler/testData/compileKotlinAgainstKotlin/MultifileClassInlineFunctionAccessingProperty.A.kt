@file:[JvmName("Test") JvmMultifileClass]

val property = "K"

inline fun K(body: () -> String): String =
        body() + property
