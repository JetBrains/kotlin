// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME

import kotlin.reflect.KProperty
import kotlin.test.assertEquals

object Delegate {
    lateinit var prop: KProperty<*>

    operator fun provideDelegate(thiz: Any?, p: KProperty<*>): Delegate {
        prop = p
        return this
    }

    operator fun getValue(x: Any?, p: KProperty<*>) {
        assertEquals(prop as Any, p)
        assertEquals(p as Any, prop)
        assertEquals(p.hashCode(), prop.hashCode())
        assertEquals("property x (Kotlin reflection is not available)", p.toString())
    }
}

val x: Unit by Delegate

fun box(): String {
    x
    return "OK"
}
