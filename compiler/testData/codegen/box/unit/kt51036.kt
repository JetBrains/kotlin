// WITH_STDLIB
// IGNORE_BACKEND: WASM
//  ^ Unresolved reference: synchronized

fun box(): String {
    A().close()
    return "OK"
}

class A {
    companion object;
    operator fun String.invoke() = Unit
    fun close() = synchronized(this) { "abc" }()
}
