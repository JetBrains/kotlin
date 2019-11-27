// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK

package test

interface Test<T> {
    fun test(p: T): T {
        return null!!
    }

    fun foo(p: T): T {
        return null!!
    }
}

interface Test2: Test<String> {
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
    checkNoMethod(Test2::class.java, "test", Any::class.java)

    checkMethodExists(TestClass::class.java, "test", Any::class.java)
    checkMethodExists(TestClass::class.java, "test", String::class.java)
    checkMethodExists(TestClass::class.java, "foo", Any::class.java)
    checkMethodExists(TestClass::class.java, "foo", String::class.java)


    val test2DefaultImpls = java.lang.Class.forName("test.Test2\$DefaultImpls")
    checkMethodExists(test2DefaultImpls, "test", Test2::class.java, String::class.java)
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
