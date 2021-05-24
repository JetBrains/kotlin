// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

import kotlin.reflect.KProperty

val a by <!RECURSION_IN_IMPLICIT_TYPES!>a<!>

val b by Delegate(b)

val c by d
val d by <!RECURSION_IN_IMPLICIT_TYPES!>c<!>

class Delegate(i: Int) {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
