// FIR_IDENTICAL
// WITH_STDLIB
// WITH_REFLECT

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class SettingType<out V : Any>(
    val type : KClass<out V>
)

class SettingReference<V : Any, T : SettingType<V>>(
    var t : T,
    var v : V
)

class IdeWizard {
    var projectTemplate by setting(SettingReference(SettingType(42::class), 42))

    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
        object : ReadWriteProperty<Any?, V?> {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
                if (value == null) return
                reference.t = SettingType(value::class) as T
                reference.v = value
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): V? {
                return reference.v
            }
        }
}
