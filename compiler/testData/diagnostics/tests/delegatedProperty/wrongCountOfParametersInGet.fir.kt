// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  val a: Int by Delegate(<!NO_VALUE_FOR_PARAMETER!>)<!>
}

val aTopLevel: Int by Delegate(<!NO_VALUE_FOR_PARAMETER!>)<!>

class Delegate {
  fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}
