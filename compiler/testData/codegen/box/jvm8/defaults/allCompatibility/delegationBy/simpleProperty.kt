// !JVM_DEFAULT_MODE: all-compatibility
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    @JvmDefault
    val test: String
        get() = "O"

    val testDelegated: String
        get() = "fail"

}

class Delegate : Test {
    override val test: String
        get() = "fail"

    override val testDelegated: String
        get() = "K"
}

class TestClass(val foo: Test) : Test by foo

fun box(): String {
    val testClass = TestClass(Delegate())
    return testClass.test + testClass.testDelegated
}
