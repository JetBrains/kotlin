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

interface Test3 : Test2 {

}

fun box(): String {
    try {
        Test2::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "fail 1"
    }

    try {
        Test3::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "OK"
    }
    return "fail 2"
}