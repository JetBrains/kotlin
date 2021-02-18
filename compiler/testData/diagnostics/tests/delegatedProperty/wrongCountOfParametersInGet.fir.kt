// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  val a: Int by <!INAPPLICABLE_CANDIDATE!>Delegate<!>()
}

val aTopLevel: Int by <!INAPPLICABLE_CANDIDATE!>Delegate<!>()

class Delegate {
  fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}
