// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by Delegate()

class Delegate {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}