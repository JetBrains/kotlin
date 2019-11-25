// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun foo() = "OK"
}

class B : T {
    inner class C {
        fun bar() = (T::foo)(this@B)
    }
}

fun box() = B().C().bar()
