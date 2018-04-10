// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "fail"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME
abstract class TestClass : Test {
    abstract override fun test(): String
}

interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return "OK"
    }
}


class TestClass2 : TestClass(), Test2 {
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return TestClass2().test()
}
