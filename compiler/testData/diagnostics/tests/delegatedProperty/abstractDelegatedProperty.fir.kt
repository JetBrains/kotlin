// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

abstract class A {
    abstract val a: Int by Delegate()
}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
