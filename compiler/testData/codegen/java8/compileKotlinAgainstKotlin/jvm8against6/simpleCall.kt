// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS
class TestClass : Test {
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return TestClass().test()
}
