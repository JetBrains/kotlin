// TARGET_BACKEND: JVM

// WITH_STDLIB

private data class C(val status: String = "OK")

fun box(): String {
    val c = (C::class.java.getConstructor().newInstance())
    return c.status
}
