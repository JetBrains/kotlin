// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun box(): String {
        class Local : Inner() {
            val u = foo()
        }
        val u = Local().u
        return if (u == 42) "OK" else "Fail $u"
    }

    open inner class Inner
    fun foo() = 42
}

fun box() = A().box()
