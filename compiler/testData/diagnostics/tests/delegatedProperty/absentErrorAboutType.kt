// !DIAGNOSTICS: -UNUSED_PARAMETER

val a by Delegate()

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}