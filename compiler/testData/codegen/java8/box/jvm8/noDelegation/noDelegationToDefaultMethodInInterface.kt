// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
// WITH_RUNTIME
// FULL_JDK
interface Test {
    fun test() {
    }
}

interface Test2 : Test {

}

fun box(): String {
    try {
        Test2::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "OK"
    }
    return "fail"
}