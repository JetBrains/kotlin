// ISSUE: KT-67772 K2: Metadata misses NoInfer annotation for unsafeCast result
// SEPARATE_SIGNATURE_DUMP_FOR_K2

// IGNORE_BACKEND: JS_IR
// REASON: KT-69567 Missing source annotation @UnsafeVariance on `unsafeVariance ()` symbol usage in `main()`

// MODULE: lib
// FILE: lib.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

fun exact():          @kotlin.internal.Exact   String = "OK"
fun noInfer():        @kotlin.internal.NoInfer String = "OK"
fun unsafeVariance(): @kotlin.UnsafeVariance   String = "OK"

val extensionFunctionType: @kotlin.ExtensionFunctionType Function1<Int, Unit> = {}

// MODULE: main(lib)
// FILE: main.kt
fun main() {
    val mainExact = exact()
    val mainNoInfer = noInfer()
    val mainUnsafeVariance = unsafeVariance()

    val mainExtensionFunctionType = extensionFunctionType
}
