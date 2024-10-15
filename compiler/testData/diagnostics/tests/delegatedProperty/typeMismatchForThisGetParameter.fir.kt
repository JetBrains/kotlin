// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class B {
  val b: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()
}

val bTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()

class A

class Delegate {
  fun getValue(t: A, p: KProperty<*>): Int {
    return 1
  }
}
