// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public class J extends K {
}

// FILE: K.kt

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

public open class K {
    var prop: String = ":("

    val Int.ext: Int get() = this
}

fun box(): String {
    val j = J()

    val prop = J::prop
    if (prop !is KMutableProperty1<*, *>) return "Fail instanceof"
    if (prop.name != "prop") return "Fail name: ${prop.name}"
    if (prop.get(j) != ":(") return "Fail get before: ${prop.get(j)}"
    prop.set(j, ":)")
    if (prop.get(j) != ":)") return "Fail get after: ${prop.get(j)}"


    if (prop == K::prop) return "Fail J::prop == K::prop (these are different properties)"


    val klass = J::class
    if (klass.declaredMemberProperties.isNotEmpty()) return "Fail: declaredMemberProperties should be empty"
    if (klass.declaredMemberExtensionProperties.isNotEmpty()) return "Fail: declaredMemberExtensionProperties should be empty"

    val prop2 = klass.memberProperties.firstOrNull { it.name == "prop" } ?: "Fail: no 'prop' property in memberProperties"
    if (prop != prop2) return "Fail: property references from :: and from properties differ: $prop != $prop2"
    if (prop2 !is KMutableProperty1<*, *>) return "Fail instanceof 2"
    (prop2 as KMutableProperty1<J, String>).set(j, "::)")
    if (prop.get(j) != "::)") return "Fail get after 2: ${prop.get(j)}"


    val ext = klass.memberExtensionProperties.firstOrNull { it.name == "ext" } ?: "Fail: no 'ext' property in memberExtensionProperties"
    ext as KProperty2<J, Int, Int>
    val fortyTwo = ext.get(j, 42)
    if (fortyTwo != 42) return "Fail ext get: $fortyTwo"

    return "OK"
}
