// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

abstract class A {
    abstract val a: Int by <!ABSTRACT_DELEGATED_PROPERTY!>Delegate()<!>
}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
