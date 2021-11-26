// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    private val foo: String
        get() = "O"

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
