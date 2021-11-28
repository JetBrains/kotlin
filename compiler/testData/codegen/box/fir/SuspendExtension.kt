// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// MODULE: lib
// FILE: A.kt

class CoroutineScope

suspend fun <T> runWithTimeout(
    block: suspend CoroutineScope.() -> T
): T? = null

// MODULE: main(lib)
// FILE: B.kt

suspend fun foo(): Boolean = runWithTimeout {
    false
} ?: true

fun box(): String = "OK"