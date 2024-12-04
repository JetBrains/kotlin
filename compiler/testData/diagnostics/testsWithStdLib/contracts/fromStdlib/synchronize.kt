// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +ReadDeserializedContracts +UseCallsInPlaceEffect

fun test(lock: Any) {
    val x: Int

    synchronized(lock) {
        x = 42
    }

    x.inc()
}