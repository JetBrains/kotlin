// FILE: 1.kt
inline fun run(f: () -> Unit) = f()
inline fun withAny(f: Any.() -> Unit) = Any().f()
inline fun foo(x: Any?, y: Any?, f: () -> Unit) = f()

// FILE: 2.kt

fun box(): String {
    run outer@{
        withAny inner@{
            foo(null, 0 ?: return@outer) {
                foo(null, null ?: return@inner) {}
            }
        }
    }
    return "OK"
}