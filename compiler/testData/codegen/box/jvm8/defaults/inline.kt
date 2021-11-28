// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

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
