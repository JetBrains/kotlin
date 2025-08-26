// FILE: A.kt
private annotation class Annotation

private fun foo(@Annotation x: String = "O", y: String = x) = y

private fun bar(x: String = "K", @Annotation y: String = x) = y

internal inline fun baz() = foo() + bar()

// FILE: B.kt
fun box(): String {
    return baz()
}
