// MODULE: lib
// !JVM_DEFAULT_MODE: disable
// FILE: 1.kt

interface Test {
    fun test(): String {
        return "OK"
    }
}

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: 2.kt
interface Test2 : Test {
    override fun test(): String {
        return super.test()
    }
}


class TestClass : Test2 {

}

fun box(): String {
    return TestClass().test()
}
