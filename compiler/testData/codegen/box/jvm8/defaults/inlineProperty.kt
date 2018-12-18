// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
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
