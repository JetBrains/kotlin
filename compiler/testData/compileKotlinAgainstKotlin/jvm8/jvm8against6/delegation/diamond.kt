// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return super.test()
    }
}

interface Test3 : Test {
    override fun test(): String
}


interface Test4 : Test2, Test3 {
    @JvmDefault
    override fun test(): String {
        return super.test()
    }
}

class TestClass : Test4 {

}


fun box(): String {
    return TestClass().test()
}
