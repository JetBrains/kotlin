// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var log = ""

    var s: Any? = null
    for (t in arrayOf("1", "2", "3")) {
        class A() {
            fun foo() = { t }
        }

        if (s == null) {
            s = A()
        }

        log += (s as A).foo()()
    }

    if (log != "111") return "fail1: ${log}"

    s = null
    log = ""
    for (t in arrayOf("1", "2", "3")) {
        class B() {
            val y = t

            fun foo() = { y }
        }

        if (s == null) {
            s = B()
        }

        log += (s as B).foo()()
    }

    if (log != "111") return "fail2: ${log}"

    return "OK"
}