// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK

interface Test {
    @JvmDefault
    fun test(): String {
        return "Test"
    }
}

open class TestClass : Test {

}


interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return "Test2"
    }
}

interface Test3 : Test2 {

}


class TestClass2 : TestClass(), Test3 {

}

fun box(): String {
    val test = TestClass2().test()
    if (test != "Test2") return "fail 1: $test"
    checkNoMethod(TestClass::class.java, "test")
    checkNoMethod(Test3::class.java, "test")
    checkNoMethod(TestClass2::class.java, "test")

    return "OK"
}

fun checkNoMethod(clazz: Class<*>, name: String) {
    try {
        clazz.getDeclaredMethod(name)
    }
    catch (e: NoSuchMethodException) {
        return
    }
    throw AssertionError("fail " + clazz)
}
