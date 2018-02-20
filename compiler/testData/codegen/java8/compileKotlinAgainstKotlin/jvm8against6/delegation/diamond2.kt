// !API_VERSION: 1.3
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME
open class TestClass : Test {
    override fun test(): String {
        return super.test()
    }
}

interface Test2 : Test {
    @kotlin.annotations.JvmDefault
    override fun test(): String
}


class TestClass2 : TestClass(), Test2 {
    override fun test(): String {
        return super<TestClass>.test()
    }
}

fun box(): String {
    return TestClass().test()
}