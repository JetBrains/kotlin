// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
