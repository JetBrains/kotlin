// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface KRunnable {
    fun run()
}

fun runnable(kr: KRunnable) = kr

fun foo() {}

fun box(): String {
    val foo1 = runnable(::foo)
    val foo2 = runnable(::foo)

    if (foo1 != foo2) {
        return "Failed: foo1 != foo2"
    }

    return "OK"
}