// IGNORE_BACKEND: JS_IR
class A {
    public lateinit var str: String
}

fun box(): String {
    val a = A()
    try {
        a.str
    } catch (e: RuntimeException) {
        return "OK"
    }
    return "FAIL"
}