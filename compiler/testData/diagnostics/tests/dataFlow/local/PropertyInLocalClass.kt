fun test(x: Any?) {
  if (x !is String) return
  class C {
    val v = x.length
  }
  object {
    val v = x.length
  }
}