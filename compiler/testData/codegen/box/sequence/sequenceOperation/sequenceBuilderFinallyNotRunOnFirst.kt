// WITH_STDLIB
var log = ""
fun box(): String {
    val r = sequence {
        try {
            yield(1)
            yield(2)
        } finally {
            log += "f"
        }
    }.first()
    return if (r == 1 && log == "") "OK" else "Fail: $r, $log"
}
