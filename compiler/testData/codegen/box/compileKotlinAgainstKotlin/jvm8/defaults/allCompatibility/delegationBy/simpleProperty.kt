// !JVM_DEFAULT_MODE: all-compatibility
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

// MODULE: lib
// FILE: 1.kt
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

// MODULE: main(lib)
// FILE: 2.kt
class TestClass(val foo: Test) : Test by foo

fun box(): String {
    val testClass = TestClass(Delegate())
    return testClass.test + testClass.testDelegated
}
