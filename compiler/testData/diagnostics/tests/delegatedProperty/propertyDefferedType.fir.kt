// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

import kotlin.reflect.KProperty

class B {
    val c by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Delegate<!>(<!UNRESOLVED_REFERENCE!>ag<!>)<!>
}

class Delegate<T: Any>(val init: T) {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = null!!
}
