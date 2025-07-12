// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline val <reified T> T.id: T
    get() = (this as Any) as T

// FILE: main.kt
fun foo(x: (String) -> String) = x("OK")

fun box(): String {
    return foo(String::id)
}
