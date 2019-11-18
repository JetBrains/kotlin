// IGNORE_BACKEND_FIR: JVM_IR

fun bar(b: ()-> Unit) { b() }

class C() {
    val p: Int = run {
        var v = 10
        bar() {
            v = 20
        }
        v + 1
    }
}

fun box(): String {
    val c = C()
    if (c.p != 21) return "fail ${c.p}"
    return "OK"
}
