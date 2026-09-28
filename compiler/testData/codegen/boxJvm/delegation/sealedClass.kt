// TARGET_BACKEND: JVM
// WITH_STDLIB

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}

fun box(): String {
    return A.B("OK").c.toString()
}
