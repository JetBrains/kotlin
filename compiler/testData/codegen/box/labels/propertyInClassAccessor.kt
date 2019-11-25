// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    val Int.innerGetter: Int
        get() {
            return this@innerGetter
        }

    fun test(): Int {
        val i = 1
        if (i.innerGetter != 1) return 0
        return 1
    }
}

fun box(): String {
    if (Test().test() != 1) return "inner getter or setter failed"
    return "OK"
}
