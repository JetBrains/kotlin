// MODULE: lib
// IGNORE_BACKEND_K1: NATIVE
// REASON: ClassicFrontendFacade.performNativeModuleResolve expects DependencyKind.Source, not DependencyKind.Binary
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
