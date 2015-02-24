import kotlin.reflect.*
import kotlin.reflect.jvm.*

public open class K {
    var prop: String = ":("

    val Int.ext: Int get() = this
}

fun box(): String {
    val j = J()

    val prop = J::prop
    if (prop !is KMutableMemberProperty<*, *>) return "Fail instanceof"
    if (prop.name != "prop") return "Fail name: ${prop.name}"
    if (prop.get(j) != ":(") return "Fail get before: ${prop[j]}"
    prop[j] = ":)"
    if (prop.get(j) != ":)") return "Fail get after: ${prop[j]}"


    if (prop == K::prop) return "Fail J::prop == K::prop (these are different properties)"


    val klass = javaClass<J>().kotlin
    val prop2 = klass.getProperties().firstOrNull { it.name == "prop" }
                ?: "Fail: no 'prop' property in getProperties()"
    if (prop != prop2) return "Fail: property references from :: and from getProperties() differ"
    if (prop2 !is KMutableMemberProperty<*, *>) return "Fail instanceof 2"
    (prop2 as KMutableMemberProperty<J, String>).set(j, "::)")
    if (prop.get(j) != "::)") return "Fail get after 2: ${prop[j]}"


    val ext = klass.getExtensionProperties().firstOrNull { it.name == "ext" }
              ?: "Fail: no 'ext' property in getProperties()"
    ext as KMemberExtensionProperty<J, Int, Int>
    val fortyTwo = ext.get(j, 42)
    if (fortyTwo != 42) return "Fail ext get: $fortyTwo"

    return "OK"
}
