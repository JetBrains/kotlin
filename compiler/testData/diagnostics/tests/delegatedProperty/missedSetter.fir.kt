// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int by A()

class A {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
      return 1
    }
}
