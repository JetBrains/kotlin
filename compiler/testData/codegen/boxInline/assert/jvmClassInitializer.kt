// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: inline.kt
package test

class A {
    inline fun doAssert() {
        assert(false)
    }
}

// FILE: inlineSite.kt
import test.*

class B {
    companion object {
        @JvmField
        val triggered: Boolean = try {
            A().doAssert()
            false
        } catch (e: AssertionError) {
            true
        }
    }
}

class Dummy

fun box(): String {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(false)
    return if (loader.loadClass("B").getField("triggered").get(null) == true)
        "FAIL: assertion triggered"
    else
        "OK"
}
