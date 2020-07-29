// !API_VERSION: 1.2
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ReleaseCoroutines
// !WITH_NEW_INFERENCE
// SKIP_TXT

suspend fun dummy() {}

// TODO: Forbid
fun builder(c: suspend () -> Unit) {}

suspend fun test1() {
    kotlin.coroutines.coroutineContext

    kotlin.coroutines.experimental.coroutineContext

    suspend {}()

    dummy()

    val c: suspend () -> Unit = {}
    c()

    builder {}
}

fun test2() {
    kotlin.coroutines.experimental.buildSequence<Int> {
        yield(1)
    }
    kotlin.sequences.buildSequence<Int> {
        yield(1)
    }
}

suspend fun test3(): Unit = kotlin.coroutines.experimental.suspendCoroutine { _ -> Unit }

suspend fun test4(): Unit = kotlin.coroutines.suspendCoroutine { _ -> Unit }
