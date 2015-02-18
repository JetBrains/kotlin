fun test(x: Any) {
  if (x !is String) return

  class LocalOuter {
    fun foo(y: Any) {
      if (y !is String) return
      class Local {
        init {
          <!DEBUG_INFO_SMARTCAST!>x<!>.length()
          <!DEBUG_INFO_SMARTCAST!>y<!>.length()
        }
      }
    }
  }
}
