// !DIAGNOSTICS: -UNUSED_PARAMETER

val a by Delegate()

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}