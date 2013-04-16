var b: Int by Delegate()

class Delegate {
    fun get(t: Any?, p: String): Int {
      t.equals(p) // to avoid UNUSED_PARAMETER warning
      return 1
    }

    fun set(t: Any?, p: String, i: Int): Int {
      t.equals(p) // to avoid UNUSED_PARAMETER warning
      return i
    }
}