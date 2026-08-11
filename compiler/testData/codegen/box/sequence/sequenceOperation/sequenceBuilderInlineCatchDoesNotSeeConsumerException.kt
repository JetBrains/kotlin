// WITH_STDLIB
var log = ""
inline fun guarded(block: () -> Unit) {
    try {
        block()
    } catch (e: IllegalStateException) {
        log += "c"
    }
}
fun box(): String {
    try {
        for (x in sequence { guarded { yield(1) } }) {
            error("consumer")
        }
    } catch (e: IllegalStateException) {
        log += "o"
    }
    return if (log == "o") "OK" else "Fail: $log"
}
