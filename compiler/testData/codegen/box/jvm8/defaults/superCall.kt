// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }
}

interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return super.test()
    }
}


class TestClass : Test2 {

}


fun box(): String {
    return TestClass().test()
}
