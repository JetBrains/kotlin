// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

class Delegate(val value: String) {
    operator fun getValue(instance: Any?, property: KProperty<*>) = value
}

class Foo

val Foo.bar: String by Delegate("Foo")
val String.bar: String by Delegate("String")
val Unit.bar: String by Delegate("Unit")

class MemberExtensions {
    val Foo?.bar: String by Delegate("Foo")
    val String?.bar: String by Delegate("String")
    val Unit?.bar: String by Delegate("Unit")
}

fun box(): String {
    val foo = Foo()

    assertEquals("Foo", ((foo::bar).apply { isAccessible = true }.getDelegate() as Delegate).value)
    assertEquals("Foo", ((Foo::bar).apply { isAccessible = true }.getDelegate(foo) as Delegate).value)
    assertEquals("String", ((""::bar).apply { isAccessible = true }.getDelegate() as Delegate).value)
    assertEquals("String", ((String::bar).apply { isAccessible = true }.getDelegate("") as Delegate).value)
    assertEquals("Unit", ((Unit::bar).apply { isAccessible = true }.getDelegate() as Delegate).value)

    val me = MemberExtensions::class.members.filter { it.name == "bar" } as List<KProperty2<MemberExtensions, Any?, String>>
    assertEquals(listOf("Foo", "String", "Unit"), me.sortedBy {
        it.extensionReceiverParameter!!.type.toString()
    }.map {
        (it.apply { isAccessible = true }.getDelegate(MemberExtensions(), null) as Delegate).value
    })

    return "OK"
}
