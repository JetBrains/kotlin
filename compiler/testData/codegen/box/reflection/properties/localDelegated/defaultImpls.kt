// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.test.assertEquals

object Delegate {
    operator fun getValue(z: Any?, p: KProperty<*>): String? {
        assertEquals("val x: kotlin.String?", p.toString())
        return "OK"
    }
}

interface Foo {
    fun bar(): String {
        val x by Delegate
        return x!!
    }
}

object O : Foo

fun box(): String = O.bar()
