// LANGUAGE: +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JVM

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun foo1(s: SuspendRunnable) {}
fun bar1() {}

fun bar2(s: String = ""): Int = 0

fun bar3() {}
suspend fun bar3(s: String = ""): Int = 0

fun box(): String {
    foo1(::bar1)
    foo1(::bar2)
    foo1(::bar3)

    return "OK"
}
