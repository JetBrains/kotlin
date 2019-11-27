// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

interface Test {
    fun test(): String {
        return "OK"
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
