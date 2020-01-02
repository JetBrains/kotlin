fun test(x: Any) {
  if (x !is String) return

  class LocalOuter {
    inner class Local {
      init {
        x.<!UNRESOLVED_REFERENCE!>length<!>
      }
    }
  }
}
