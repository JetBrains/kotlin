// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
// WITH_RUNTIME
// FULL_JDK
interface Test {
    fun test() {
    }
}

class TestClass : Test {

}

fun box(): String {
    try {
        TestClass::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "OK"
    }
    return "fail"
}