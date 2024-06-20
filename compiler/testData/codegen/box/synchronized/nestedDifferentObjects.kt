// TARGET_BACKEND: JVM
// WITH_STDLIB

class Monitor

fun box(): String {
    val obj = Monitor() as java.lang.Object
    val obj2 = Monitor() as java.lang.Object

    synchronized (obj) {
        synchronized (obj2) {
            obj.wait(1)
            obj2.wait(1)
        }
    }

    return "OK"
}
