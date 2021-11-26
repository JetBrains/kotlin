// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

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
