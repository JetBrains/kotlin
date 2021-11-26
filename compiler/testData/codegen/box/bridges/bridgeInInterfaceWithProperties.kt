// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

package test

interface Test<T> {
    var T.foo: T
        get() = null!!
        set(value) {
            null!!
        }
}

interface Test2 : Test<String> {

    override var String.foo: String
        get() = ""
        set(value) {}
}

class TestClass : Test2

fun box(): String {
    checkNoMethod(Test2::class.java, "setFoo", Any::class.java, Any::class.java)

    checkMethodExists(TestClass::class.java, "getFoo",  String::class.java)
    checkMethodExists(TestClass::class.java, "getFoo", Any::class.java)
    checkMethodExists(TestClass::class.java, "setFoo", Any::class.java, Any::class.java)
    checkMethodExists(TestClass::class.java, "setFoo", String::class.java, String::class.java)

    val test2DefaultImpls = java.lang.Class.forName("test.Test2\$DefaultImpls")
    checkMethodExists(test2DefaultImpls, "getFoo", Test2::class.java, String::class.java)
    checkMethodExists(test2DefaultImpls, "setFoo", Test2::class.java, String::class.java, String::class.java)

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
