// JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    fun test(): String {
        return inlineFun { "OK" }
    }

    private inline fun inlineFun(s: () -> String) = s()
}

class TestClass : Test {

}

fun box(): String {
    val foo = TestClass()
    return foo.test()
}
