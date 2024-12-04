// TARGET_BACKEND: JVM
// WITH_STDLIB

class Monitor

fun box(): String {
    val obj = Monitor() as java.lang.Object

    synchronized (obj) {
        synchronized (obj) {
            obj.wait(1)
        }
    }

    return "OK"
}
