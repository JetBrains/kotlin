// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

interface Test<T> {
    fun test(p: T): T {
        return p
    }
}

class TestClass : Test<String> {
    override fun test(p: String): String {
        return p + "K"
    }
}

fun <T> execute(t: Test<T>, p: T): T {
    return t.test(p)
}

fun box(): String {
    return execute(TestClass(), "O")
}
