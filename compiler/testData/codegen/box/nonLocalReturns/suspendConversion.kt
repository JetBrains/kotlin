// ISSUE: KT-72464
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// SKIP_UNBOUND_IR_SERIALIZATION
// IGNORE_BACKEND: WASM
// Should be fixed in WASM as side effect of KT-74392
// When fixed, please remove LANGUAGE and SKIP_UNBOUND_IR_SERIALIZATION directives and remove the test `t67024WithInlinedFunInKlib.kt`
// ^^^ Cannot deserialize inline fun: FUN name:foo visibility:public modality:FINAL returnType:kotlin.String [inline,suspend]
// IGNORE_INLINER: IR

// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend inline fun foo(f: suspend () -> String) = f()

suspend inline fun bar(f: () -> String) = foo(f)

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
