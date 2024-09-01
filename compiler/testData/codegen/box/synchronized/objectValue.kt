// TARGET_BACKEND: JVM
// WITH_STDLIB

class Monitor

fun box(): String {
    var obj = Monitor() as java.lang.Object
    val result = synchronized (obj) {
        "239"
    }

    if (result != "239") return "Fail: $result"

    return "OK"
}
