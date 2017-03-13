// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

private data class C(val status: String = "OK")

fun box(): String {
    val c = (C::class.java.getConstructor().newInstance())
    return c.status
}
