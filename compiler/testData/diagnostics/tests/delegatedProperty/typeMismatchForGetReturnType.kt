// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val c: Int by <!NI;DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, OI;DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): String {
    return ""
  }
}
