// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
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

