// IGNORE_BACKEND: NATIVE
// REASON: native tests use source dependencies and JVM tests use binary dependencies, so source annotations are invisble here

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
