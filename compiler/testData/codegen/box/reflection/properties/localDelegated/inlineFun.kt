// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals

object Delegate {
    lateinit var property: KProperty<*>

    operator fun getValue(instance: Any?, kProperty: KProperty<*>) {
        property = kProperty
    }
}

class Foo {
    inline fun foo() {
        val x by Delegate
        x
    }
}

fun box(): String {
    Foo().foo()
    assertEquals("val x: kotlin.Unit", Delegate.property.toString())
    return "OK"
}
