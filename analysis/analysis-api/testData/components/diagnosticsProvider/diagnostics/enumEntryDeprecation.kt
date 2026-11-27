// FILE: E.kt
package p1

enum class E {
    @Deprecated("", level = DeprecationLevel.ERROR)
    DeprecatedEntry
}

// FILE: usage.kt
package p1

fun usage() {
    val _ = E.DeprecatedEntry
}
