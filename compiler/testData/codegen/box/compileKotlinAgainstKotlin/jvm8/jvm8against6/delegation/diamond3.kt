// !JVM_DEFAULT_MODE: enable
// MODULE: lib
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "fail"
    }
}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: 2.kt
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
