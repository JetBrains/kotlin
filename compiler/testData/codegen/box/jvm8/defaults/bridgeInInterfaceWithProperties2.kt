// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

interface Test<T> {
    @JvmDefault
    var T.test: T
        get() = null!!
        set(value) {
            null!!
        }

    var T.foo: T
        get() = null!!
        set(value) {
            null!!
        }
}

interface Test2 : Test<String> {
    @JvmDefault
    override var String.test: String
        get() = ""
        set(value) {}

    override var String.foo: String
        get() = ""
        set(value) {}
}

class TestClass : Test2

fun box(): String {
    checkMethodExists(Test2::class.java, "getTest", String::class.java)
    checkMethodExists(Test2::class.java, "getTest", Any::class.java)
    checkMethodExists(Test2::class.java, "setTest", Any::class.java, Any::class.java)
    checkMethodExists(Test2::class.java, "setTest", String::class.java, String::class.java)

    checkNoMethod(Test2::class.java, "setFoo", Any::class.java, Any::class.java)

    checkNoMethod(TestClass::class.java, "getTest", String::class.java)
    checkNoMethod(TestClass::class.java, "getTest", Any::class.java)
    checkNoMethod(TestClass::class.java, "setTest", Any::class.java, Any::class.java)
    checkNoMethod(TestClass::class.java, "setTest", String::class.java, String::class.java)

    checkMethodExists(TestClass::class.java, "getFoo", String::class.java)
    checkMethodExists(TestClass::class.java, "getFoo", Any::class.java)
    checkMethodExists(TestClass::class.java, "setFoo", Any::class.java, Any::class.java)
    checkMethodExists(TestClass::class.java, "setFoo", String::class.java, String::class.java)

    val test2DefaultImpls = java.lang.Class.forName("Test2\$DefaultImpls")
    checkNoMethod(test2DefaultImpls, "getTest", String::class.java)
    checkNoMethod(test2DefaultImpls, "getTest", Any::class.java)
    checkNoMethod(test2DefaultImpls, "setTest", Any::class.java, Any::class.java)
    checkNoMethod(test2DefaultImpls, "setTest", String::class.java, String::class.java)

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
