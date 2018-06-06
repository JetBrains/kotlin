// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

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
