// JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK

// Before KT-73954 is resolved, this test is checking that no overrides of 'test' are generated in TestClass, TestClass2 or Test3.
// CHECK_BYTECODE_LISTING

interface Test {
    fun test(): String = "Test"
}

open class TestClass : Test

interface Test2 : Test {
    override fun test(): String = "OK"
}

interface Test3 : Test2

class TestClass2 : TestClass(), Test3

fun box(): String {
    return TestClass2().test()
}
