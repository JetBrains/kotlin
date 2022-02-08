// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// WITH_STDLIB
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
