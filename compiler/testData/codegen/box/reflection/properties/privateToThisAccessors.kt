// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

class K<in T : String> {
    private var t: T
        get() = "OK" as T
        set(value) {}

    fun run(): String {
        val p = K::class.memberProperties.single() as KMutableProperty1<K<String>, String>
        p.isAccessible = true
        p.set(this as K<String>, "")
        return p.get(this) as String
    }
}

fun box() = K<String>().run()
