// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

// MODULE: lib
// FILE: 1.kt
interface Test {
    val test: String get() = "Fail"
}

class Delegate : Test {
    override val test: String get() = "OK"
}

// MODULE: main(lib)
// FILE: 2.kt
class TestClass(val foo: Test) : Test by foo

fun box(): String {
    val testClass = TestClass(Delegate())
    return testClass.test
}
