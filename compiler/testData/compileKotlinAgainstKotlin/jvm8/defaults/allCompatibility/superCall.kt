// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
class TestClass : Test {
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return TestClass().test()
}
