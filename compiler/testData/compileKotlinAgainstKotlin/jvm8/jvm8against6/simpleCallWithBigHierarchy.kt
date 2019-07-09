// !JVM_DEFAULT_MODE: enable
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return super.test()
    }
}

interface Test3 : Test2 {

}
class TestClass : Test3

fun box(): String {
    return TestClass().test()
}
