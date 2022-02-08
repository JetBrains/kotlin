// !LANGUAGE: +SuspendConversion
// SKIP_KT_DUMP

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun foo(s: SuspendRunnable) {}

fun bar(): () -> Unit = {}

fun test(f: () -> Unit) {
    foo(f)
    foo(bar())
    var t = f
    foo(t)
}
