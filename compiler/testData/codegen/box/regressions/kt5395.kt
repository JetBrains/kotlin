// IGNORE_BACKEND_FIR: JVM_IR
class D {
    companion object {
        protected val F: String = "OK"
    }

    inner class E {
        fun foo() = F
    }
}

fun box(): String {
    return D().E().foo()
}
