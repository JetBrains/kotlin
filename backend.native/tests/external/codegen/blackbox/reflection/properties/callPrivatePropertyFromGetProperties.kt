// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*

class K(private val value: String)

fun box(): String {
    val p = K::class.java.kotlin.memberProperties.single() as KProperty1<K, String>

    try {
        return p.get(K("Fail: private property should not be accessible by default"))
    }
    catch (e: IllegalCallableAccessException) {
        // OK
    }

    p.isAccessible = true

    return p.get(K("OK"))
}
