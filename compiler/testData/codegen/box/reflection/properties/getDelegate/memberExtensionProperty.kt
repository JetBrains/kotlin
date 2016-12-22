// IGNORE_BACKEND: JS
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

object Delegate {
    var storage = ""
    operator fun getValue(instance: Any?, property: KProperty<*>) = storage
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) { storage = value }
}

class Bar

class Foo {
    var Bar.result: String by Delegate
}

fun box(): String {
    val foo = Foo()
    val bar = Bar()
    with(foo) { bar.result = "Fail" }
    val prop = Foo::class.members.single { it.name == "result" } as KMutableProperty2<Foo, Bar, String>
    val d = prop.apply { isAccessible = true }.getDelegate(foo, bar) as Delegate
    with(foo) { bar.result = "OK" }
    assertEquals(d, prop.apply { isAccessible = true }.getDelegate(foo, bar))
    return d.getValue(foo, prop)
}
