fun test(x: Any) {
  if (x !is String) return

  class LocalOuter {
    fun foo(y: Any) {
      if (y !is String) return
      class Local {
        {
          x.length
          y.length
        }
      }
    }
  }
}

