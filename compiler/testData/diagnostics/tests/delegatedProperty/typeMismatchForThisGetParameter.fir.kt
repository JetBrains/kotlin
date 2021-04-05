// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class B {
  val b: Int by <!ARGUMENT_TYPE_MISMATCH!>Delegate()<!>
}

val bTopLevel: Int by <!ARGUMENT_TYPE_MISMATCH!>Delegate()<!>

class A

class Delegate {
  fun getValue(t: A, p: KProperty<*>): Int {
    return 1
  }
}
