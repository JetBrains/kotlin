// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 1.kt
interface Test {
    @JvmDefault
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
