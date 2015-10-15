// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class B {
  val b: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

val bTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class A

class Delegate {
  fun getValue(t: A, p: KProperty<*>): Int {
    return 1
  }
}
