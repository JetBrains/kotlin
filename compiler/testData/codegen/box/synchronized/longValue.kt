// WITH_RUNTIME

fun box(): String {
    var obj = "0" as java.lang.Object
    val result = synchronized (obj) {
        239L
    }

    if (result != 239L) return "Fail: $result"

    return "OK"
}
