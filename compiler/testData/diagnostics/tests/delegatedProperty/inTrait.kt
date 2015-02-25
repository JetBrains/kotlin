// !DIAGNOSTICS: -UNUSED_PARAMETER

trait T {
  val a: Int <!DELEGATED_PROPERTY_IN_TRAIT!>by Delegate()<!>
}

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}
