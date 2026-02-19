// FILE: lib.kt
inline fun <reified T> isNullable() = null is T

// FILE: main.kt
fun box(): String =
        if (isNullable<String?>()) "OK" else "Fail"