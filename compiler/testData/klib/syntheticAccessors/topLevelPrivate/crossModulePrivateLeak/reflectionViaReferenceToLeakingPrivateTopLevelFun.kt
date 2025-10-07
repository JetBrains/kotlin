// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM
// TARGET_BACKEND: NATIVE, JS_IR, WASM
// ^^^ JVM has a similar but distinct behavior w.r.t. references to unreferencable callables

// Synthetic accessors are generated only to allow "calling" a declaration, they are not quite sufficient to provide
// reflection information (such as .name). So a request for it is supposed to fail at runtime with a PL linage error.
// However, because of techical limitation, the original declaration (with a correct information) may still slip
// through sometimes. So we expect either a valid answare or a runtime error.

// MODULE: lib
// FILE: A.kt
private fun privateFun() = "bad"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = ::privateFun

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    try {
        return publicInlineFunction().name.let { if (it == "privateFun") "OK" else it }
    } catch (linkageError: Error) {
        return if (linkageError::class.simpleName == "IrLinkageError") "OK" else linkageError.toString()
    }
}
