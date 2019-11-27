// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
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
