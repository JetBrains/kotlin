// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val Int.a by Delegate(this)

class A {
  val Int.a by <!UNRESOLVED_REFERENCE!><!INAPPLICABLE_CANDIDATE!>Delegate<!>(this)<!>
}

class Delegate(i: Int) {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
