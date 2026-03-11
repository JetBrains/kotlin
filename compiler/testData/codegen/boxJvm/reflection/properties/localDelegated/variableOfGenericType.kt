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
        fun <Z> classMember() {
            val delegate = Delegate<Map<Pair<X, Y>, Z>>(emptyMap())
            val c: Map<Pair<X, Y>, Z> by delegate
            c

            val map = delegate.property.returnType
            assertEquals("kotlin.collections.Map<kotlin.Pair<X, Y>, Z>", map.toString())

            val pair = map.arguments.first().type!!
            val x = pair.arguments.first().type!!
            assertEquals("X", x.classifier.toString())

            // They should be equal, but currently it works incorrectly, see KT-82319.
            assertNotEquals(A::class.typeParameters.single(), x.classifier)
        }
    }
}

fun <W> topLevel(): W? {
    val delegate = Delegate<List<W>>(emptyList())
    val d: List<W> by delegate
    d

    val list = delegate.property.returnType
    assertEquals("kotlin.collections.List<W>", list.toString())

    val w = list.arguments.first().type!!
    assertEquals("W", w.classifier.toString())

    val ref: KFunction0<Float?> = ::topLevel
    assertNotEquals(ref.typeParameters.single(), w.classifier)

    return null
}

fun box(): String {
    A<String>().B<Int>().classMember<Double>()
    topLevel<Float>()
    return "OK"
}
