// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828
interface Test {
    val test: String get() = "Fail"
}

class Delegate : Test {
    override val test: String get() = "OK"
}

class TestClass(val foo: Test) : Test by foo

fun box(): String {
    val testClass = TestClass(Delegate())
    return testClass.test
}
