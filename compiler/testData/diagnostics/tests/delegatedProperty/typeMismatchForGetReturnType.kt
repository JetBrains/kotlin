// !DIAGNOSTICS: -UNUSED_PARAMETER

val c: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): String {
    return ""
  }
}
