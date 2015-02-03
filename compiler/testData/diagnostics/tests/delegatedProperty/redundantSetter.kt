// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by Delegate()
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = 1<!>
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>set(i) {}<!>

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }

  fun set(t: Any?, p: PropertyMetadata, i: Int) {}
}
