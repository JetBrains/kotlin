// LANGUAGE: +SuspendConversion

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun foo(s: SuspendRunnable) {}

fun test(f: () -> Unit) {
    foo { }
    foo(f)
}

fun box(): String {
    test({ "" })
    return "OK"
}