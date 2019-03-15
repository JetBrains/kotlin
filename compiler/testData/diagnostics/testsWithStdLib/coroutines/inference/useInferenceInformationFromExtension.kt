// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

// FILE: annotation.kt

package kotlin

annotation class BuilderInference

// FILE: test.kt

class GenericController<T> {
    suspend fun yield(t: T) {}
}

suspend fun <S> GenericController<S>.extensionYield(s: S) {}

@BuilderInference
suspend fun <S> GenericController<S>.safeExtensionYield(s: S) {}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

val normal = generate {
    yield(42)
}

val extension = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    extensionYield("foo")
}

val safeExtension = generate {
    safeExtensionYield("foo")
}