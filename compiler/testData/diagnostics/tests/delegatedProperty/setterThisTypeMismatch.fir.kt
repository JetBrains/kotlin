// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class D {
  var c: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()
}

var cTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()

class A

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: A, p: KProperty<*>, i: Int) {}
}
