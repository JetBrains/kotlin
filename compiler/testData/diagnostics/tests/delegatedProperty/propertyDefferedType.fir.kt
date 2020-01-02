// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

import kotlin.reflect.KProperty

class B {
    val c by <!INAPPLICABLE_CANDIDATE!>Delegate<!>(<!UNRESOLVED_REFERENCE!>ag<!>)
}

class Delegate<T: Any>(val init: T) {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = null!!
}
