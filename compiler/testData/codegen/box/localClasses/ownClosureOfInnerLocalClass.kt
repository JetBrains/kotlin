fun box(): String {
    var log = ""

    var s: Any? = null
    for (t in arrayOf("1", "2", "3")) {
        class C() {
            val y = t

            inner class D() {
                fun foo() = "($y;$t)"
            }
        }

        if (s == null) {
            s = C()
        }

        log += (s as C).D().foo()
    }

    if (log != "(1;1)(1;1)(1;1)") return "fail: ${log}"

    return "OK"
}
