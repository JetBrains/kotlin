import kotlin.reflect.*
import kotlin.reflect.jvm.*

class K(private val value: String)

fun box(): String {
    val p = javaClass<K>().kotlin.memberProperties.single() as KProperty1<K, String>

    try {
        return p.get(K("Fail: private property should not be accessible by default"))
    }
    catch (e: IllegalPropertyAccessException) {
        // OK
    }

    p.isAccessible = true

    return p.get(K("OK"))
}
