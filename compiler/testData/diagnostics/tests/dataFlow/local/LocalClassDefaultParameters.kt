fun test(x: Any) {
  if (x !is String) return

  class Local(s: String = <!DEBUG_INFO_AUTOCAST!>x<!>) {
    fun foo(s: String = <!DEBUG_INFO_AUTOCAST!>x<!>): String = s
  }
}

