var a: Int by Delegate()
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = 1<!>
  <!ACCESSOR_FOR_DELEGATED_PROPERTY!>set(i) {}<!>

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }

  fun set(t: Any?, p: PropertyMetadata, i: Int) {
    t.equals(p) || i.equals(null)  // to avoid UNUSED_PARAMETER warning
  }
}
