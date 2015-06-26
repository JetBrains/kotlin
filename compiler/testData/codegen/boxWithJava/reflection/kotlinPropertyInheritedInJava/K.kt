import kotlin.reflect.*
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
    if (prop.get(j) != ":(") return "Fail get before: ${prop[j]}"
    prop[j] = ":)"
    if (prop.get(j) != ":)") return "Fail get after: ${prop[j]}"


    if (prop == K::prop) return "Fail J::prop == K::prop (these are different properties)"


    val klass = javaClass<J>().kotlin
    if (klass.declaredProperties.isNotEmpty()) return "Fail: declaredProperties should be empty"
    if (klass.declaredExtensionProperties.isNotEmpty()) return "Fail: declaredExtensionProperties should be empty"

    val prop2 = klass.properties.firstOrNull { it.name == "prop" } ?: "Fail: no 'prop' property in properties"
    if (prop != prop2) return "Fail: property references from :: and from properties differ: $prop != $prop2"
    if (prop2 !is KMutableProperty1<*, *>) return "Fail instanceof 2"
    (prop2 as KMutableProperty1<J, String>).set(j, "::)")
    if (prop.get(j) != "::)") return "Fail get after 2: ${prop[j]}"


    val ext = klass.extensionProperties.firstOrNull { it.name == "ext" } ?: "Fail: no 'ext' property in extensionProperties"
    ext as KProperty2<J, Int, Int>
    val fortyTwo = ext.get(j, 42)
    if (fortyTwo != 42) return "Fail ext get: $fortyTwo"

    return "OK"
}
