// !LANGUAGE: +SuspendConversion
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: ChainedFunSuspendConversionForSimpleExpressionKt$box$1 cannot be cast to kotlin.jvm.functions.Function1

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