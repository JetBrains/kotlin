// ISSUE: KT-72464, KT-91139
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:1.9,2.0,2.1,2.2
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: JS,Wasm-JS,Wasm-WASI:1.9,2.0,2.1,2.2
// KT-91139: KLIBs built by compilers up to 2.4.10 without rich callable references, which is the default up to 2.2.21,
// store the suspend conversion of `f` in `bar` under the legacy SUSPEND_CONVERSION origin name. A consumer which
// doesn't recognize it loses the non-local return (JS, Wasm) or crashes in the backend (Native).
// See also compiler/testData/codegen/box/suspendConversion/legacyKlibSuspendConversionOfLambda.kt

// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
suspend inline fun foo(f: suspend () -> String) = f()

suspend inline fun bar(f: () -> String) = foo(f)

// FILE: main.kt
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun test(): String {
    bar { return "OK" }
    return "Fail"
}

fun box(): String {
    var r = "0"
    ::test.startCoroutine(object : Continuation<String> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<String>) {
            r = result.getOrThrow()
        }
    })
    return r
}
