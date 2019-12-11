fun test(x: Any?) {
  if (x !is String) return

  class C {
    val v = x.<!UNRESOLVED_REFERENCE!>length<!>

    val vGet: Int
      get() = x.<!UNRESOLVED_REFERENCE!>length<!>

    val s: String = x
  }
}
