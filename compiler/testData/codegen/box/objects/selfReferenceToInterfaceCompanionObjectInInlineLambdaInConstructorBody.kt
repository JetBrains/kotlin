// IGNORE_BACKEND_FIR: JVM_IR
interface Test {
    companion object {
        fun ok() = "OK"
        val x = run { Test.ok() }
        fun test() = x
    }
}

fun box() = Test.test()