// !JVM_DEFAULT_MODE: enable
// MODULE: lib
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 2.kt
open class TestClass : Test {
    override fun test(): String {
        return super.test()
    }
}

interface Test2 : Test {
    @JvmDefault
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
