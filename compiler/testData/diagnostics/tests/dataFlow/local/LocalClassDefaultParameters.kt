fun test(x: Any) {
  if (x !is String) return

  class Local(<!UNUSED_PARAMETER!>s<!>: String = <!DEBUG_INFO_SMARTCAST!>x<!>) {
    fun foo(s: String = <!DEBUG_INFO_SMARTCAST!>x<!>): String = s
  }
}

