// !DIAGNOSTICS: -UNUSED_PARAMETER

interface T {
  val a: Int <!DELEGATED_PROPERTY_IN_TRAIT!>by Delegate()<!>
}

class Delegate {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}
