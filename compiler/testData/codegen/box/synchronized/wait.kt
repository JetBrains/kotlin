// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

class Monitor

fun box(): String {
    val obj = Monitor() as java.lang.Object
    try {
        obj.wait(1)
        return "Fail: exception should have been thrown"
    }
    catch (e: IllegalMonitorStateException) {
        // OK
    }

    synchronized (obj) {
        obj.wait(1)
    }

    return "OK"
}
