// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK

interface Test<T> {
    fun test(p: T): T {
        return null!!
    }
}

interface Test2: Test<String> {
    override fun test(p: String): String {
        return p
    }
}

class TestClass : Test2

fun box(): String {
    checkMethodExists(Test2::class.java, "test", Any::class.java)
    checkMethodExists(Test2::class.java, "test", String::class.java)

    checkNoMethod(TestClass::class.java, "test", String::class.java)
    checkNoMethod(TestClass::class.java, "test", Any::class.java)

    try {
        val test2DefaultImpls = java.lang.Class.forName("Test2\$DefaultImpls")
        checkNoMethod(test2DefaultImpls, "test", String::class.java)
        checkNoMethod(test2DefaultImpls, "test", Any::class.java)
        checkNoMethod(test2DefaultImpls, "test", Test2::class.java, Any::class.java)
        checkNoMethod(test2DefaultImpls, "test", Test2::class.java, Any::class.java)
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
