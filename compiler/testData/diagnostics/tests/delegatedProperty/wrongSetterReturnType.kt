// !DIAGNOSTICS: -UNUSED_PARAMETER

var b: Int by Delegate()

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
      return 1
    }

    fun set(t: Any?, p: PropertyMetadata, i: Int): Int {
      return i
    }
}