// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

interface T {
  val a: Int by <!DELEGATED_PROPERTY_IN_INTERFACE!>Delegate()<!>
}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
