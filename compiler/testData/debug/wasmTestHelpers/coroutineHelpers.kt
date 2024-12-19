// This file is compiled into each stepping test.

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private object EmptyContinuation: Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}


@kotlin.wasm.WasmExport
@Suppress("WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE")
fun makeEmptyContinuation(): Continuation<Any?> = EmptyContinuation
