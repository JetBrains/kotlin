// !LANGUAGE: +SuspendConversion
// IGNORE_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62855

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