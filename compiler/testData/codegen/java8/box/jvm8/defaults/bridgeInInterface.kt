// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test<T> {
    @kotlin.annotations.JvmDefault
    fun test(p: T): T {
        return p
    }
}

interface Test2: Test<String> {
    @kotlin.annotations.JvmDefault
    override fun test(p: String): String {
        return p + "K"
    }
}

class TestClass : Test2 {
}

fun <T> execute(t: Test<T>, p: T): T {
    return t.test(p)
}

fun box(): String {
    return execute(TestClass(), "O")
}
