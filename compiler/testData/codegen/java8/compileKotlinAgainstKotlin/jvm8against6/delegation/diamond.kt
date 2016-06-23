// WITH_REFLECT
// FULL_JDK

// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
interface Test2 : Test {

}

interface Test3 : Test {

}


interface Test4 : Test2, Test3 {

}

class TestClass : Test4 {

}


fun box(): String {
    checkPresent(Test2::class.java, "test")
    checkPresent(Test3::class.java, "test")
    // TODO: enable this test once the required behavior is specified
    //checkNoMethod(Test4::class.java, "test")

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
