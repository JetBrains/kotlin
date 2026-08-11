// WITH_STDLIB
var log = ""
fun box(): String {
    try {
        sequence {
            try {
                yield(1)
            } catch (e: IllegalStateException) {
                log += "c"
            }
        }.forEach { error("consumer") }
    } catch (e: IllegalStateException) {
        log += "o"
    }
    return if (log == "o") "OK" else "Fail: $log"
}
