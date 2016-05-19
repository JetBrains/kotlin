// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
interface Test {
    fun test(): String {
        return "OK"
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}