// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

interface Test<T> {
    @JvmDefault
    fun test(p: T): T {
        return null!!
    }

    fun foo(p: T): T {
        return null!!
    }
}

interface Test2: Test<String> {
    @JvmDefault
    override fun test(p: String): String {
        return p
    }

    override fun foo(p: String): String {
        return p
    }
}

class TestClass : Test2

fun box(): String {
    checkNoMethod(Test2::class.java, "foo", Any::class.java)
    checkMethodExists(Test2::class.java, "test", Any::class.java)

    checkNoMethod(TestClass::class.java, "test", Any::class.java)
    checkMethodExists(TestClass::class.java, "foo", String::class.java)
    checkMethodExists(TestClass::class.java, "foo", Any::class.java)


    val test2DefaultImpls = java.lang.Class.forName("Test2\$DefaultImpls")
    checkNoMethod(test2DefaultImpls, "test", Any::class.java)
    checkNoMethod(test2DefaultImpls, "test", String::class.java)
    checkMethodExists(test2DefaultImpls, "foo", Test2::class.java, String::class.java)

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
