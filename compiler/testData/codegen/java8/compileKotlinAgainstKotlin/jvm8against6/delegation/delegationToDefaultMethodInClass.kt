// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test() {
    }
}

// FILE: 2.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
class TestClass : Test {

}

fun box(): String {
    try {
        TestClass::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "fail"
    }
    return "OK"
}