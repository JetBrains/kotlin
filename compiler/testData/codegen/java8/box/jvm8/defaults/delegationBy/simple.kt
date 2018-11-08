// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return "O"
    }

    fun delegatedTest(): String {
        return "fail"
    }
}

class Delegate : Test {
    override fun test(): String {
        return "Fail"
    }

    override fun delegatedTest(): String {
        return "K"
    }
}

class TestClass(val foo: Test) : Test by foo

fun box(): String {
    val testClass = TestClass(Delegate())
    return testClass.test() + testClass.delegatedTest()
}
