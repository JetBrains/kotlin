// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun box(): String {
    try {
        val a:ULong = 1u
        a as Int
        func(a)
    } catch (e: ClassCastException) {
        return "OK"
    }
    return "FAIL"
}

fun func(para: Int) {}
