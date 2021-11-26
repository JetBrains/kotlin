// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK

interface Test<T> {
    var T.test: T
        get() = null!!
        set(value) {
            null!!
        }
}

interface Test2 : Test<String> {
    override var String.test: String
        get() = ""
        set(value) {}
}

class TestClass : Test2

fun box(): String {
    checkMethodExists(Test2::class.java, "getTest", String::class.java)
    checkMethodExists(Test2::class.java, "getTest", Any::class.java)
    checkMethodExists(Test2::class.java, "setTest", Any::class.java, Any::class.java)
    checkMethodExists(Test2::class.java, "setTest", String::class.java, String::class.java)

    checkNoMethod(TestClass::class.java, "getTest", String::class.java)
    checkNoMethod(TestClass::class.java, "getTest", Any::class.java)
    checkNoMethod(TestClass::class.java, "setTest", Any::class.java, Any::class.java)
    checkNoMethod(TestClass::class.java, "setTest", String::class.java, String::class.java)

    try {
        val test2DefaultImpls = java.lang.Class.forName("Test2\$DefaultImpls")
        checkNoMethod(test2DefaultImpls, "getTest", Test2::class.java, String::class.java)
        checkNoMethod(test2DefaultImpls, "getTest", Test2::class.java, Any::class.java)
        checkNoMethod(test2DefaultImpls, "setTest", Test2::class.java, Any::class.java, Any::class.java)
        checkNoMethod(test2DefaultImpls, "setTest", Test2::class.java, String::class.java, String::class.java)
    } catch (e: ClassNotFoundException) {
        //or no class
    }

    return "OK"
}

fun checkNoMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>) {
    try {
        clazz.getDeclaredMethod(name, *parameterTypes)
    }
    catch (e: NoSuchMethodException) {
        return
    }
    throw AssertionError("fail: method $name was found in " + clazz)
}

fun checkMethodExists(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>) {
    try {
        clazz.getDeclaredMethod(name, *parameterTypes)
        return
    }
    catch (e: NoSuchMethodException) {
        throw AssertionError("fail: method $name was not found in " + clazz, e)
    }

}
