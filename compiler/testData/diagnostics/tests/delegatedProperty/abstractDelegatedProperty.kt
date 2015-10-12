// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

abstract class A {
    abstract val a: Int <!ABSTRACT_DELEGATED_PROPERTY!>by Delegate()<!>
}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
