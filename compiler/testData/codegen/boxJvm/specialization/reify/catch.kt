// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified E : Throwable> getCaught(cb: () -> Unit): E? {
    try {
        cb()
        return null
    } catch (e: E) {
        return e
    }
}

inline fun throws(cb: () -> Unit): Boolean {
    try {
        cb()
        return false
    } catch (err: Throwable) {
        return true
    }
}

fun box(): String {
    if (getCaught<Exception> { error("catch me") }?.message != "catch me") return "fail: 1"
    if (!throws { getCaught<Error> { error("catch me") } }) return "fail: 2"
    return "OK"
}
