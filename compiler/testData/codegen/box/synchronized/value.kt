// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box(): String {
    var obj = "0" as java.lang.Object
    val result = synchronized (obj) {
        239
    }

    if (result != 239) return "Fail: $result"

    return "OK"
}
