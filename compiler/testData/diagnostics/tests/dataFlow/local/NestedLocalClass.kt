fun test(x: Any) {
  if (x !is String) return

  class LocalOuter {
    inner class Local {
      {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
      }
    }
  }
}

