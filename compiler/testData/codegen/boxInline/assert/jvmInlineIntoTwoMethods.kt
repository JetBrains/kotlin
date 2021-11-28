// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: inline.kt
package test

class A {
    inline fun assert(message: String): Nothing {
        assert(false) { message }
        throw IllegalStateException("unreachable")
    }
}

// FILE: inlineSite.kt
import test.*

class Checker {
    fun o(): Nothing = A().assert("O")
    fun k(): Nothing = A().assert("K")
}

class Dummy

fun box(): String {
    var c = Dummy::class.java.classLoader.apply {
        setDefaultAssertionStatus(true)
    }.loadClass("Checker").newInstance() as Checker
    val o = try { c.o() } catch (e: AssertionError) { e.message }
    val k = try { c.k() } catch (e: AssertionError) { e.message }
    return o + k
}
