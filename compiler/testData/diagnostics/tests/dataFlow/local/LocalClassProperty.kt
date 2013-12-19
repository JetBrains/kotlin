fun test(x: Any?) {
  if (x !is String) return

  class C {
    val v = <!DEBUG_INFO_AUTOCAST!>x<!>.length

    val vGet: Int
      get() = <!DEBUG_INFO_AUTOCAST!>x<!>.length

    val s: String = <!DEBUG_INFO_AUTOCAST!>x<!>
  }
}