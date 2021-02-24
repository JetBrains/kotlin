// MODULE: lib
// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: 2.kt
class TestClass : Test {
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return TestClass().test()
}
