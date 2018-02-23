// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

var storage = "fail"

interface Test {

    @kotlin.annotations.JvmDefault
    private var foo: String
        get() = storage
        set(value) {
            storage = value
        }

    @kotlin.annotations.JvmDefault
    private fun bar(): String {
        return "K"
    }

    @kotlin.annotations.JvmDefault
    fun call(): String {
        return {
            foo = "O"
            foo + bar()
        } ()
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().call()
}
