// ISSUE: KT-61978
// WITH_REFLECT
// WITH_STDLIB

// IGNORE_INLINER: IR
// java.lang.UnsupportedOperationException
// (this function has a reified type parameter and thus can only be inlined at compilation time, not called directly)
// at Klass.<init>(DelegationByFunctionWithEnumUpperBound.kt:23)

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class Enumeration { OK }

inline fun <reified T: Enum<T>> delegate() = object: ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? = Enumeration.OK as T?
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {}
}

class Klass {
    var enumeration: Enumeration? by delegate()
}

fun box(): String {
    return Klass().enumeration.toString()
}
