// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
// WITH_RUNTIME
// FULL_JDK
interface Test {
    fun test() {
    }
}

interface Test2 : Test {

}

interface Test3 : Test2 {

}

fun box(): String {
//    try {
//        Test3::class.java.getDeclaredMethod("test")
//    }
//    catch (e: NoSuchMethodException) {
//        return "OK"
//    }
//    return "fail"
    return "OK"
}