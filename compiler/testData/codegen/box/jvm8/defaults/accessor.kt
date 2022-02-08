// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

var storage = "fail"

interface Test {

    @JvmDefault
    private var foo: String
        get() = storage
        set(value) {
            storage = value
        }

    @JvmDefault
    private fun bar(): String {
        return "K"
    }

    @JvmDefault
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
