// WITH_STDLIB
// FILE: 1.kt

import test.A
import kotlin.test.assertEquals

open class B : A() {
    fun box(): String {
        val overriddenMethod: () -> String = {
            method()
        }
        assertEquals("C.method", overriddenMethod())

        val superMethod: () -> String = {
            super.method()
        }
        assertEquals("A.method", superMethod())

        val overriddenPropertyGetter: () -> String = {
            property
        }
        assertEquals("C.property", overriddenPropertyGetter())

        val superPropertyGetter: () -> String = {
            super.property
        }
        assertEquals("A.property", superPropertyGetter())

        val overriddenPropertySetter: () -> Unit = {
            property = ""
        }
        overriddenPropertySetter()

        val superPropertySetter: () -> Unit = {
            super.property = ""
        }
        superPropertySetter()

        assertEquals("C.property;A.property;", state)

        return "OK"
    }
}

class C : B() {
    override fun method() = "C.method"
    override var property: String
        get() = "C.property"
        set(value) { state += "C.property;" }
}

fun box() = C().box()

// FILE: 2.kt

package test

abstract class A {
    public var state = ""

    // These implementations should not be called, because they are overridden in C

    protected open fun method(): String = "A.method"

    protected open var property: String
        get() = "A.property"
        set(value) { state += "A.property;" }
}
