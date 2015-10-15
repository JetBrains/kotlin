// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int by Delegate()
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = 1<!>
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>set(i) {}<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }

  operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {}
}
