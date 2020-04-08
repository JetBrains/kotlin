// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

import kotlin.reflect.KProperty

val a by <!UNRESOLVED_REFERENCE!>a<!>

val b by Delegate(b)

val c by <!UNRESOLVED_REFERENCE!>d<!>
val d by <!UNRESOLVED_REFERENCE!>c<!>

class Delegate(i: Int) {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
