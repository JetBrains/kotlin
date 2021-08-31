// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
inline fun withO(block: String.() -> String) = "O".block()

// FILE: 2.kt
interface I {
    val k: String

    fun foo() = withO(fun String.(): String { return this + k })
}

fun box(): String = object : I {
    override val k = "K"
}.foo()
