import kotlin.reflect.KProperty

class DummyDelegate<V>(val s: V) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return s
    }
}

fun testImplicit(c: C) = c.implicit.length // (1)
fun testExplicit(c: C) = c.explicit.length

open class A {
    val implicit by DummyDelegate("hello")

    val explicit: String by DummyDelegate("hello")
}

interface B {
    val implicit: String
    val explicit: String
}

class C : A(), B
