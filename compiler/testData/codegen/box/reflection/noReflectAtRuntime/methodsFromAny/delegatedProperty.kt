// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

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
