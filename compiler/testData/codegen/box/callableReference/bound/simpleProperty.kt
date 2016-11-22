// Enable when callable references to builtin members are supported.
// IGNORE_BACKEND: JS

fun box(): String {
    val f = "kotlin"::length
    val result = f.get()
    return if (result == 6) "OK" else "Fail: $result"
}
