// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by Delegate()
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = 1<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
