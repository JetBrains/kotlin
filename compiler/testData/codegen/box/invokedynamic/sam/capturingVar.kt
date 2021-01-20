// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface KRunnable {
    fun run()
}

fun runIt(r: KRunnable) {
    r.run()
}

fun box(): String {
    var test = "Failed"
    runIt { test = "OK" }
    return test
}

