// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    private var i : Int
        get() = 1
        set(i) {}

    fun foo() {
        fun f() {
            i = 2
        }
        f()
    }
}

fun box(): String {
    Test().foo()
    return "OK"
}