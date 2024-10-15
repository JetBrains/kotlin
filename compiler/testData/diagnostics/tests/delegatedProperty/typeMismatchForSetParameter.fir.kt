// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  var a: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()
}

var aTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: Any?, p: KProperty<*>, i: String) {}
}
