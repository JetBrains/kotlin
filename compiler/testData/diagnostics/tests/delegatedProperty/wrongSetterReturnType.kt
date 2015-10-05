// !DIAGNOSTICS: -UNUSED_PARAMETER

var b: Int by Delegate()

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int {
      return 1
    }

    fun setValue(t: Any?, p: PropertyMetadata, i: Int): Int {
      return i
    }
}