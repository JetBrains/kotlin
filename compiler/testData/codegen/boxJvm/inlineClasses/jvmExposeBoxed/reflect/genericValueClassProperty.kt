// WITH_REFLECT
// FULL_JDK
// TARGET_BACKEND: JVM
// java.beans is not available on Android
// IGNORE_BACKEND: ANDROID

// @JvmExposeBoxed generates unmangled boxed accessors, which is what makes a class with a value-class
// typed `var` usable as a Java bean - only `getA-<hash>` exists otherwise.
// The generic value class also guards the signature substitution from KT-71061 / KT-52706.

@file:OptIn(ExperimentalStdlibApi::class)

import java.beans.Introspector
import java.beans.PropertyDescriptor

@JvmInline
value class ValueClass<T>(val a: List<T>)

@JvmExposeBoxed
class PropContainer(var a: ValueClass<String>, var b: String)

fun box(): String {
    val exposed = PropertyDescriptor("a", PropContainer::class.java)
    if (exposed.readMethod?.name != "getA") return "FAIL exposed read method: ${exposed.readMethod}"
    if (exposed.writeMethod?.name != "setA") return "FAIL exposed write method: ${exposed.writeMethod}"
    if (exposed.propertyType != ValueClass::class.java) return "FAIL exposed property type: ${exposed.propertyType}"

    val names = Introspector.getBeanInfo(PropContainer::class.java).propertyDescriptors.map { it.name }
    if ("a" !in names) return "FAIL property 'a' not discovered: $names"

    val container = PropContainer(ValueClass(listOf("FAIL")), "")
    exposed.writeMethod.invoke(container, ValueClass(listOf("OK")))
    @Suppress("UNCHECKED_CAST")
    return (exposed.readMethod.invoke(container) as ValueClass<String>).a.single()
}
