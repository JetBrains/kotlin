// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: NATIVE
// WITH_STDLIB
// XXX!: On Native, this test both fails and doesn't fail on TC.

fun box(): String {
    try {
        val v1: Any = Array<Int>(10) {it}
        val v2 = v1 as Array<String>
        v2.forEach {
            println(it)
        }
        return "FAIL: expected exception"
    } catch (e: Throwable) {
        return "OK"
    }
}
