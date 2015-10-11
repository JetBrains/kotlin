// !DIAGNOSTICS: -UNUSED_PARAMETER

val a by Delegate()

class Delegate {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}