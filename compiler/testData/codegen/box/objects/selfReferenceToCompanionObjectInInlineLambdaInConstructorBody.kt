// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
class Test {
    companion object {
        fun ok() = "OK"
        val x = run { Test.ok() }
        fun test() = x
    }
}

fun box() = Test.test()