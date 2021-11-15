// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// !JVM_DEFAULT_MODE: all
// FILE: Foo.kt

interface Foo {
    fun toOverride(): String = "fail"
    
    fun nonOverride(): String = "K"
}

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: disable
// !JVM_DEFAULT_ALLOW_NON_DEFAULT_INHERITANCE
// FILE: main.kt

interface Derived : Foo {
    override fun toOverride(): String {
        return "O"
    }
}

class DerivedClass : Derived 


fun box(): String {
    checkMethodExists(DerivedClass::class.java, "toOverride")
    checkNoMethod(DerivedClass::class.java, "nonOverride")

    val value = DerivedClass()
    return value.toOverride() + value.nonOverride()
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
