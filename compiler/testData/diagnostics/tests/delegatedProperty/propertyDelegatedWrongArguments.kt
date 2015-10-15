// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }

    fun propertyDelegated() {}

    fun propertyDelegated(a: Int) {}

    fun propertyDelegated(a: String) {}

    fun propertyDelegated(p: KProperty<*>, a: Int) {}
}
