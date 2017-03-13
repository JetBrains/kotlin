// FILE: 1.kt
inline fun run(f: () -> Unit) = f()
inline fun withAny(f: Any.() -> Unit) = Any().f()

// FILE: 2.kt
fun foo(x: Any?, y: Any?) {}

fun box(): String {
    run outer@{
        withAny inner@{
            foo(null, null ?: return@outer)
        }
    }
    return "OK"
}