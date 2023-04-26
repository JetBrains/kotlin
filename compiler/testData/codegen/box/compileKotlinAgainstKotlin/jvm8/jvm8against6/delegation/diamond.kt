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

interface Test3 : Test {
    override fun test(): String
}


interface Test4 : Test2, Test3 {
    override fun test(): String {
        return super.test()
    }
}

class TestClass : Test4 {

}


fun box(): String {
    return TestClass().test()
}
