// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
interface Test {
    fun test() {
    }
}

class TestClass : Test {

}

fun box(): String {
    // TODO: enable this test once the required behavior is specified
//    try {
//        TestClass::class.java.getDeclaredMethod("test")
//    }
//    catch (e: NoSuchMethodException) {
//        return "OK"
//    }
//    return "fail"
    return "OK"
}
