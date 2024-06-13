// JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

@JvmDefaultWithCompatibility
interface Test {
    fun test(): String = privateFun()

    private fun privateFun() = "O"

    val prop: String
        get() = "K"

    var varProp: String
        get() = "K"
        set(value) {}
}

class TestClass : Test

fun box(): String {
    val testClass = TestClass()
    return testClass.test() + testClass.varProp
}
