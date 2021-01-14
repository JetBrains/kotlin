// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface KRunnable {
    fun run()
}

fun runIt(r: KRunnable) {
    r.run()
}

var test = "Failed"

fun box(): String {
    runIt { test = "OK" }
    return test
}
