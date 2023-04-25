// WITH_STDLIB
// IGNORE_BACKEND: WASM
//  ^ Unresolved reference: synchronized
// IGNORE_BACKEND: NATIVE
//  ^ Unresolved reference: synchronized

fun box(): String {
    A().close()
    return "OK"
}

class A {
    companion object;
    operator fun String.invoke() = Unit
    @Suppress("DEPRECATION_ERROR")
    fun close() = synchronized(this) { "abc" }()
}
