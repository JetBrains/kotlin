// !LANGUAGE: +ReadDeserializedContracts +UseCallsInPlaceEffect

fun test(lock: Any) {
    val x: Int

    synchronized(lock) {
        x = 42
    }

    x.inc()
}