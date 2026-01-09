// ISSUE: KT-61978
// WITH_REFLECT
// WITH_STDLIB

// FILE: lib.kt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class Enumeration { OK }

inline fun <reified T: Enum<T>> delegate() = object: ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? = Enumeration.OK as T?
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {}
}

// FILE: main.kt
class Klass {
    var enumeration: Enumeration? by delegate()
}

fun box(): String {
    return Klass().enumeration.toString()
}
