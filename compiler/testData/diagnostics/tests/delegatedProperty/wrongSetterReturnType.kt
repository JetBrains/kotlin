// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var b: Int by Delegate()

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
      return 1
    }

    operator fun setValue(t: Any?, p: KProperty<*>, i: Int): Int {
      return i
    }
}
