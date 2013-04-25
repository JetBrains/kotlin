trait T {
  val a: Int <!DELEGATED_PROPERTY_IN_TRAIT!>by Delegate()<!>
}

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
}
