// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  val a: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()
}

val aTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()

class Delegate {
  fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}
