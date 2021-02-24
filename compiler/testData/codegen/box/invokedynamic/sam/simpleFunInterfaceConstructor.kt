// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface KRunnable {
    fun run()
}

var test = "Failed"

fun box(): String {
    KRunnable { test = "OK" }.run()
    return test
}
