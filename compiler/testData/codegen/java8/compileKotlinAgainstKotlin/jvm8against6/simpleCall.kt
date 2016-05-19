// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}