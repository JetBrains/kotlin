// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM_IR
// ^ TODO KT-37373

fun testContinue() {
    for (i in 0..1) {
        for (j in continue downTo 1u) {}
    }
}

fun box(): String {
    testContinue()
    return "OK"
}