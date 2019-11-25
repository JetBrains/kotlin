// IGNORE_BACKEND_FIR: JVM_IR
// KT-5786 NoSuchMethodError: no accessor for private fun with default arguments

class A {
    private fun foo(result: String = "OK"): String = result

    companion object {
        fun bar() = A().foo()
    }
}

fun box() = A.bar()
