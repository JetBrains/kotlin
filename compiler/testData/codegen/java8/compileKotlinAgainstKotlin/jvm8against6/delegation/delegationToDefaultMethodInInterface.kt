// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS
interface Test2 : Test {
    override fun test(): String {
        return super.test()
    }
}

class TestClass : Test2 {

}

fun box(): String {
    return TestClass().test()
}
