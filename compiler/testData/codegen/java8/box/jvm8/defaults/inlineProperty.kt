// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return inlineProp
    }

    fun testDefaultImpls(): String {
        return inlineProp
    }

    @JvmDefault
    private inline val inlineProp: String
        get() = "OK"

}

class TestClass : Test {

}

fun box(): String {
    val foo = TestClass()
    if (foo.test() != "OK") return "fail: ${foo.test()}"
    return foo.testDefaultImpls()
}
