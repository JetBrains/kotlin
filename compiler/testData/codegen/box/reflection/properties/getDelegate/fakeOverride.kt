// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = "OK"
}

open class Base {
    val x: String by Delegate
}

class Derived : Base()

fun box(): String {
    val d = Derived()
    assertEquals(
            (Base::x).apply { isAccessible = true }.getDelegate(d),
            (Derived::x).apply { isAccessible = true }.getDelegate(d)
    )
    return d.x
}
