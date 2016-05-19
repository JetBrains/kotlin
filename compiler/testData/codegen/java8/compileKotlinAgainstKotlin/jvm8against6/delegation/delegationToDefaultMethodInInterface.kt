// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test() {
    }
}

// FILE: 2.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
interface Test2 : Test {

}

fun box(): String {
    try {
        Test2::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "fail"
    }
    return "OK"
}