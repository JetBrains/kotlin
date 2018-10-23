// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
