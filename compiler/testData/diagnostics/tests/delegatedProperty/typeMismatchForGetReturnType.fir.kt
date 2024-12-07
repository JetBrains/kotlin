// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val c: Int <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>by<!> Delegate()

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): String {
    return ""
  }
}
