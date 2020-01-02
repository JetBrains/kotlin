// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int by Delegate()
  get() = 1
  set(i) {}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }

  operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {}
}
