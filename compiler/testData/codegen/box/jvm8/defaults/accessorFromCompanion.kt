// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    private val foo: String
        get() = "O"

    @JvmDefault
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
