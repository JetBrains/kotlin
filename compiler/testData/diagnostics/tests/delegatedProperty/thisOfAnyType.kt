class A {
  var a: Int by Delegate()
}

var aTopLevel: Int by Delegate()

class Delegate {
  fun get(t: Any?, p: String): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
  fun set(t: Any?, p: String, a: Int) {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    a.equals(null)
  }
}