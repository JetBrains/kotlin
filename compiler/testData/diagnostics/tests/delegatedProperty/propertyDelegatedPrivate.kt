// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }

    private fun propertyDelegated(p: KProperty<*>) {}
}
