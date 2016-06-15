// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test() {
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
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
