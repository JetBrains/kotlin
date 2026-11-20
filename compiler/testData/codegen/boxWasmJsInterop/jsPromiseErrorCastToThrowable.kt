// WITH_STDLIB
// WITH_COROUTINES

// IGNORE_BACKEND: WASM_JS, WASM_WASI
// ^ Kotlin/Wasm test infra doesn't support `suspend box()`, but using `suspend box()` is essential here, see KT-91288

import kotlin.js.Promise
import kotlin.coroutines.*
import helpers.*

fun thenPromise(
    p: JsAny?,
    ok: (JsAny?) -> Unit,
    err: (JsPromiseError) -> Unit
): Unit = js("{ p.then(ok, err); }")

fun promiseRejectString(): Promise<Nothing> = js("{ return Promise.reject('Test'); }")

fun promiseRejectNumber(): Promise<Nothing> = js("{ return Promise.reject(42); }")

fun promiseRejectTypeError(): Promise<Nothing> = js("{ return Promise.reject(new TypeError('Test')); }")

fun jsCtorName(v: JsAny?): String? = js("(v && v.constructor && v.constructor.name) || null")

// Do the same thing as kotlinx.coroutines does
// https://github.com/Kotlin/kotlinx.coroutines/blob/b872360a0d322654e8b89972aef26668245f2bbe/kotlinx-coroutines-core/js/src/Promise.js.kt#L4-L5
// In Kotlin/JS, this is a cast to the same type, which normally should always succeed, but not here!
// We MUST support this case, otherwise we'll break kotlinx.coroutines.
private fun JsPromiseError.toThrowable(): Throwable =
    this as? Throwable ?: Exception("Promise resolved with a non-Throwable exception '$this' (type ${this::class})")

private suspend fun <T: JsAny?> Promise<T>.await(): T = suspendCoroutine { cont ->
    this@await.then(
        onFulfilled = { cont.resume(it); null },
        onRejected = { cont.resumeWithException(it.toThrowable()); null })
}

suspend fun reject_string_caught_as_JsException(): Boolean {
    try {
        promiseRejectString().await()
        return false
    } catch (e: Exception) {
        return e.message == "Promise resolved with a non-Throwable exception 'Test' (type class String)"
    }
}

suspend fun reject_number_caught_as_JsException(): Boolean {
    try {
        promiseRejectNumber().await()
        return false
    } catch (e: Exception) {
        return e.message == "Promise resolved with a non-Throwable exception '42' (type class Int)"
    }
}

suspend fun reject_typeerror_preserves_message_and_type(): Boolean {
    try {
        promiseRejectTypeError().await()
        return false
    } catch (e: JsException) {
        return e.message == "Test" && jsCtorName(e.thrownValue) == "TypeError"
    }
}

suspend fun box(): String {
    if (!reject_string_caught_as_JsException()) return "reject string failed"
    else if (!reject_number_caught_as_JsException()) return "reject number failed"
    else if (!reject_typeerror_preserves_message_and_type()) return "reject TypeError failed"
    return "OK"
}
