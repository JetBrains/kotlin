// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM_8_TARGET
open class TestClass : Test {

}

interface Test2 : Test {

}


class TestClass2 : TestClass(), Test2 {

}

fun box(): String {
    checkPresent(TestClass::class.java, "test")
    checkPresent(Test2::class.java, "test")
    checkNoMethod(TestClass2::class.java, "test")

    return TestClass().test()
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