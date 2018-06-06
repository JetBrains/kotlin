// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return inlineFun { "O" }
    }

    fun testDefaultImpls(): String {
        return inlineFun { "K" }
    }

    @JvmDefault
    private inline fun inlineFun(s: () -> String) = s()

}

class TestClass : Test {

}

fun box(): String {
    val foo = TestClass()
    return foo.test() + foo.testDefaultImpls()
}
