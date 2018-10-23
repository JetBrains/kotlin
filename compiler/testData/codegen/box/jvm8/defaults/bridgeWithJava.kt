// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
// FILE: Test.java
public interface Test<T> {
    default T test(T p) {
        return null;
    }
}

// FILE: kotlin.kt

interface Test2: Test<String> {
    @JvmDefault
    override fun test(p: String): String {
        return p
    }

    fun forDefaultImpls() {}
}

class TestClass : Test2

fun box(): String {
    checkMethodExists(Test2::class.java, "test", String::class.java)
    checkMethodExists(Test2::class.java, "test", Any::class.java)

    checkNoMethod(TestClass::class.java, "test", String::class.java)
    checkNoMethod(TestClass::class.java, "test", Any::class.java)

    val test2DefaultImpls = java.lang.Class.forName("Test2\$DefaultImpls")
    checkNoMethod(test2DefaultImpls, "test", Any::class.java)
    checkNoMethod(test2DefaultImpls, "test", String::class.java)

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
