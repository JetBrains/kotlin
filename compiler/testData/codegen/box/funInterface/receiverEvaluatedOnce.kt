// WITH_STDLIB

fun interface KRunnable {
    fun invoke()
}

fun runTwice(r: KRunnable) {
    r.invoke()
    r.invoke()
}

class A() {
    fun f() {}
}

fun box(): String {
    var x = 0
    runTwice({ x++; A() }()::f)
    if (x != 1) return "Fail"
    return "OK"
}
