// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @kotlin.annotations.JvmDefault
    private val foo: String
        get() = "O"

    @kotlin.annotations.JvmDefault
    private fun bar(): String {
        return "K"
    }

    companion object {
        fun call(test: Test): String {
            return test.foo + test.bar()
        }
    }
}

class TestClass : Test

fun box(): String {
    return Test.call(TestClass())
}
