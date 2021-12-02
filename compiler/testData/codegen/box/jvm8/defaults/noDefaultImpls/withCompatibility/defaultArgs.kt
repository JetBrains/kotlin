// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
// CHECK_BYTECODE_LISTING

@JvmDefaultWithCompatibility
interface Test {
    fun test(s: String ="OK"): String {
        return s
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
