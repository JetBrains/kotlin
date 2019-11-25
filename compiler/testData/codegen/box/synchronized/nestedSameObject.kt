// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box(): String {
    val obj = "" as java.lang.Object

    synchronized (obj) {
        synchronized (obj) {
            obj.wait(1)
        }
    }

    return "OK"
}
