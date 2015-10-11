// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by Delegate()
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = 1<!>

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}