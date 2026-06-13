// TARGET_BACKEND: JVM

// FILE: 1.kt
package test

interface I {
    fun f(useCapturedInSelf: Boolean): String
}

inline fun <reified T> cast(x: Any): T = x as T

fun <T: Any> createAnotherInstance(clazz: Class<T>, capturedValue: String) : T {
    val constructor = clazz.getDeclaredConstructor(String::class.java)
    constructor.isAccessible = true
    return constructor.newInstance(capturedValue)
}

var global: String = ""

inline fun test(captured: String) = object : I {
    override fun f(useCapturedInSelf: Boolean): String = g(useCapturedInSelf)

    inline fun g(useCapturedInSelf: Boolean): String {
        val target = if (useCapturedInSelf) this else cast(createAnotherInstance(javaClass, global))
        return target.h()
    }

    inline fun h(): String = captured
}

// FILE: 2.kt

import test.*

fun box(): String {
    global = "Fail 1"
    val result1 = test("OK").f(true)
    if (result1 != "OK") return result1

    global = "OK"
    return test("Fail 2").f(false)
}
