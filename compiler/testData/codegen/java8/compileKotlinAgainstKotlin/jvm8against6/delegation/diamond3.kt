// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test(): String {
        return "fail"
    }
}

// FILE: 2.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
open class TestClass : Test {

}

interface Test2 : Test {
    override fun test(): String {
        return "OK"
    }
}


class TestClass2 : TestClass(), Test2 {

}

fun box(): String {
    checkPresent(TestClass::class.java, "test")
    checkPresent(Test2::class.java, "test")
    checkPresent(TestClass2::class.java, "test")

    return TestClass2().test()
}

fun checkNoMethod(clazz: Class<*>, name: String) {
    try {
        clazz.getDeclaredMethod(name)
    } catch (e: NoSuchMethodException) {
        return
    }
    throw java.lang.AssertionError("Method $name exists in $clazz")
}

fun checkPresent(clazz: Class<*>, name: String) {
    try {
        clazz.getDeclaredMethod(name)
    } catch (e: NoSuchMethodException) {
        throw java.lang.AssertionError("Method $name doesn't exist in $clazz")
    }
    return
}