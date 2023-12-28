// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.*

class Delegate<out T>(val value: T) {
    lateinit var property: KProperty<*>

    operator fun getValue(instance: Any?, kProperty: KProperty<*>): T {
        property = kProperty
        return value
    }
}

class A<X> {
    inner class B<Y> {
        fun <Z> foo() {
            val delegate = Delegate<Map<Pair<X, Y>, Z>>(emptyMap())
            val c: Map<Pair<X, Y>, Z> by delegate
            c

            assertEquals("kotlin.collections.Map<kotlin.Pair<X, Y>, Z>", delegate.property.returnType.toString())
        }
    }
}

fun box(): String {
    A<String>().B<Int>().foo<Double>()
    return "OK"
}
