// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
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

    fun call(): String {
        return { foo + bar()} ()
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().call()
}
