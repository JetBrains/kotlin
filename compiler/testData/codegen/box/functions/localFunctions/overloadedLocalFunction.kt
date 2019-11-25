// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var s = ""

    fun foo(x: String) {
        s += x
    }

    fun foo() {
        foo("K")
    }

    run {
        foo("O")
        foo()
    }

    return s
}