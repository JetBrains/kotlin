// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
interface Test2 : Test {

}


class TestClass : Test2 {

}

fun box(): String {
    return TestClass().test()
}
