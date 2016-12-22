// IGNORE_BACKEND: JS
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

class Delegate(val value: String) {
    operator fun getValue(instance: Any?, property: KProperty<*>) = value
}

class Foo {
    val x: String by Delegate("class")

    companion object {
        val x: String by Delegate("companion")
    }
}

fun box(): String {
    val foo = Foo()
    assertEquals("class", ((foo::x).apply { isAccessible = true }.getDelegate() as Delegate).value)
    assertEquals("class", ((Foo::x).apply { isAccessible = true }.getDelegate(foo) as Delegate).value)
    assertEquals("companion", ((Foo.Companion::x).apply { isAccessible = true }.getDelegate() as Delegate).value)
    return "OK"
}
