// !DIAGNOSTICS: -UNUSED_PARAMETER

var b: Int by Delegate()

class Delegate {
    operator fun getValue(t: Any?, p: PropertyMetadata): Int {
      return 1
    }

    operator fun setValue(t: Any?, p: PropertyMetadata, i: Int): Int {
      return i
    }
}