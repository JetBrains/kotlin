package test

// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class A(val arg: KClass<*>, val args: Array<KClass<*>>, vararg val other: KClass<*>)

A(Int::class, array(String::class), Double::class, Char::class)
class MyClass {
    throws(Exception::class)
    fun foo() {
        Exception::class
    }
}
