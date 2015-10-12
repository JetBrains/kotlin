// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by <!DELEGATE_SPECIAL_FUNCTION_AMBIGUITY!>Delegate()<!>

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }

    fun propertyDelegated(p: KProperty<*>, i: Int = 1) {}

    fun propertyDelegated(p: KProperty<*>, s: String = "") {}
}
