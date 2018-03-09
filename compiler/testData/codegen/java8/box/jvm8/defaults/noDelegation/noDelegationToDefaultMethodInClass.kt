// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
interface Test {
    @JvmDefault
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
