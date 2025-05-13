// TARGET_BACKEND: NATIVE, WASM
// ^^^ JVM has a similar but distinct behavior w.r.t. references to unreferencable callables
// ^^^ JS should be fixed by KT-76093

// Synthetic accessors are generated only to allow "calling" a declaration, they are not quite sufficient to provide
// reflection information (such as .name). So a request for it is supposed to fail at runtime with a PL linage error.
// However, because of techical limitation, the original declaration (with a correct information) may still slip
// through sometimes. So we expect either a valid answare or a runtime error.

// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = "bad"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineVar() = ::privateVar
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    try {
        return A().publicInlineVar().name.let { if (it == "privateVar") "OK" else it }
    } catch (linkageError: Error) {
        return if (linkageError::class.simpleName == "IrLinkageError") "OK" else linkageError.toString()
    }
}
